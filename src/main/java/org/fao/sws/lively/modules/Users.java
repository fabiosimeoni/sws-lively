package org.fao.sws.lively.modules;

import static java.util.Arrays.*;
import static java.util.UUID.*;

import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import org.fao.sws.domain.operational.Privilege;
import org.fao.sws.domain.plain.operational.Group;
import org.fao.sws.domain.plain.operational.Permission;
import org.fao.sws.domain.plain.operational.User;
import org.fao.sws.domain.plain.reference.DataSet;
import org.fao.sws.ejb.GroupService;
import org.fao.sws.ejb.PermissionService;
import org.fao.sws.ejb.UserService;
import org.fao.sws.lively.SwsTest.Start;
import org.fao.sws.lively.core.SecurityContext;
import org.fao.sws.model.dao.PermissionDao;
import org.fao.sws.model.filter.PermissionFilter;

@UtilityClass
public class Users extends Common {
	
	void startup(@Observes Start e, 
			
									GroupService groups, 
									UserService users, 
									PermissionService permissions, 
									PermissionDao permissionsdao) {
		
		Users.groupservice = groups;
		Users.userservice=users;
		Users.permissionservice=permissions;
		Users.permissiondao = permissionsdao;
	}
	
	
	////////////// groups //////////////////////////////////////////////////////////////////////
	
	public GroupService groupservice;
	
	
	public boolean ofAdmins = true;
	public Predicate<Group> isAdmin = g -> g.isAdministrator();
	public Predicate<Group> isRegular = isAdmin.negate();
	public Predicate<Group> isCalled(String n) { return g->g.getName().equals(n);}
	
	//one
	
	public Group group(Long id) {
		
		return groupservice.getGroup(id);
	}
	
	public Group group(String n) {
	
		return aGroupThat(isCalled(n));
	}
	
	public Group groupOf(User user) {
		
		return group(user.getUsername());
	}

	public Group aGroupThat(Predicate<Group> filter) {
		
		return oneof(allGroupsThat(filter));
	}
	
	public Group anAdminGroup() {
		//single-user makes sure groupOf() works
		return aGroupThat(isAdmin.and(g->g.isSingleUser()));
	}
	
	public Group aRegularGroup() {
		//single-user makes sure groupOf() works
		return aGroupThat(isRegular.and(g->g.isSingleUser()));
	}
	
	//many
	
	public Stream<Group> groups() {
		return groupservice.getAllGroups(true).stream();
	}
	
	public Stream<Group> allGroupsThat(Predicate<Group> filter) {
		
		return groups().filter(filter);
	}
	
	public Stream<Group> adminGroups() {
		
		return allGroupsThat(isAdmin);
	}
	
	public Stream<Group> regularGroups() {
		
		return allGroupsThat(isRegular);
	}
	
	//create
	
	public Group aNewGroup() {
		
		return aNewGroup(false);
	}

	public Group aNewGroup(boolean isadmin) {
		
		String name = "testgroup-"+randomUUID();
		
		return groupservice.create(new Group(null, name, name+" (test group)", true, isadmin, false));
	}
	
	public Stream<Group> groupsOf(User user) {
		return groupservice.getGroupsByUserId(user.getId()).stream();
	}
	
	
	
	//////////////// users /////////////////////////////////////////////////////
	
	public UserService userservice;
	
	public Predicate<User> isAdminUser = u -> groupsOf(u).filter(isAdmin).findFirst().isPresent();
	
	public Predicate<User> isRegularUser = u->groupsOf(u).filter(isAdmin.negate()).findFirst().isPresent();
	
	//one
	
	public User aUserThat(Predicate<User> filter) {
		
		return oneof(users().filter(filter));

	}

	public User user(Long id) {
		return userservice.getUser(id);
	}
	
	public User user(String name) {
		//double lookup as getUser(name) does not return groups...
		return userservice.getUser(userservice.getUser(name).getId());
	}

	
	public User aUserIn(Group group) {
		
		return oneof(usersIn(group));
	}
	
	
	public User anAdmin() {
		
		return aUserIn(anAdminGroup());

	}
	
	public User aRegularUser() {
		
		return oneof(regularUsers());

	}
	
	public User aUserWith(Permission permission) {
		
		return oneof(usersIn(permission.getGroup()));

	}
	
	
	public ToClause<Group,Void> join(User user) {
		
		return g-> {
			groupservice.addUser(user.getId(),g.getId());
			return null;
		};
		
	}
	
	
	// many
	
	public Stream<User> users() {
		
		return shuffle(userservice.getAllUsers());

	}
	
	
	public Stream<User> allUsersThat(Predicate<User> filter) {
		
		return users().filter(filter);
	}

	
	public Stream<User> admins() {
		
		return users().filter(isAdminUser);
	}
	
	public Stream<User> regularUsers() {
		
		return users().filter(isRegularUser);
	}
	
	public Stream<User> usersIn(Group group) {
		
		return userservice.getUsersByGrpId(group.getId()).stream();

	}
	
	//create
	

	public User aNewUser() {
		
		//automtically added to same-name group
		return userservice.create(new User(null,"testuser-"+randomUUID(),true));
	}
	
	public User aNewUserIn(Group group) {
		
		User user = userservice.create(new User(null,"testuser-"+randomUUID(),true));
		join(user).to(group);
		return user;
	}
	
	

	

	
	/////// login/out ///////////////////////////////////////////////////////////////////////
	

	SecurityContext sctx;
	
	public User currentuser;
	
	public void login(User user) {
		
		log.info("logging {}",user.getUsername());
		
		currentuser = user;
		
		sctx = new SecurityContext(currentuser,groupservice);
		sctx.bind();
	}
	
	public void logout() {
		
		if (sctx!=null)
			sctx.remove();
	}
	
	/**
	 * Performs an action with administrator privileges.
	 */
	@SneakyThrows
	public <T> T sudo(Callable<T> task) {
		
		User previoususer = currentuser;
		
		if (groupsOf(currentuser).filter(isAdmin).findFirst().isPresent())
			return task.call();

		login(anAdmin());
		
		T t = task.call();
			
		login(previoususer);
		
		return t;
		
	}
	
	/**
	 * Performs an action with administrator privileges.
	 */
	public void sudo(Runnable task) {
		
		//adapts to callabale
		sudo(()->{
			task.run();
			return null;
		});
		
	}
	

	////////////// permissions //////////////////////////////////////////////////////////////////////

	
	public PermissionService permissionservice;
	public PermissionDao permissiondao;
	
	
	public Predicate<Permission> withPrivileges(Privilege ... privileges) {
		return p -> p.getPrivileges().containsAll(asList(privileges));
	}
	
	public Predicate<Permission> withoutPrivileges(Privilege ... privileges) {
		return withPrivileges(privileges).negate();
	}
	
	public Predicate<Permission> over(String name) {
		return p->p.getDataSet().getName().equals(name);
	}
	
	
	public Permission permission(Long id) {
		return permissionservice.getPermission(id);
	}
	
	public Stream<Permission> permissionsOf(Group g) {
		return permissionservice.getPermissionByGrpId(g.getId()).stream();
	}

	public Stream<Permission> permissionsOf(Stream<Group> groups) {
		
		return 	groups.flatMap(g->permissionsOf(g)).filter(p->!p.isAdmin());
	}
	
	public Stream<Permission> permissionsOver(String datasetname) {
	
		PermissionFilter filter = new PermissionFilter();
		filter.setDataSetCode(datasetname);
		
		return 	permissionservice.getGroupsPermission(filter).stream();
	}
	

	
	public ToClause<Group,OverClause<DataSet,Permission>> assign(Privilege ... privileges) {
		
		return g -> ds -> {
		
				Permission perm = new Permission(null,ds, ds.getName()+"-permission-"+randomUUID());
				
				perm.setGroup(g);
				
				perm.setPrivileges(new HashSet<>(asList(privileges)));
				
				//creates, reloads, and returns
				return permission(permissionservice.create(with(perm)).getId());
				
				
			};

	}; 
	
	public ToClause<Permission,Permission> add(Privilege ... privileges) {
		
		return p -> {
		
				p.getPrivileges().addAll(asList(privileges));
				
				//change and reload
				return permission(permissionservice.update(with(p)).getId());
				
			};

	}; 
	
	public FromClause<Permission,Permission> revoke(Privilege ... privileges) {
		
		return p -> {
				
				p.getPrivileges().removeAll(asList(privileges));
				
				//change and reload
				return permission(permissionservice.update(with(p)).getId());
				
			};

	}

	
	public PermissionFilter with(Permission p) {
		
		//hard to believe...
		
		PermissionFilter filter = new PermissionFilter();
		filter.setId(p.getId());
		filter.setDomainCode(p.getDataSet().getDomain().getCode());
		filter.setDataSetCode(p.getDataSet().getName());
		filter.setDimension2ids(p.getDimension2ids());
		filter.setSessionDescription(p.getDescription());
		filter.setApprove(p.isApprove());
		filter.setFreeze(p.isFreeze());
		filter.setGroupId(p.getGroup().getId());
		filter.setPublish(p.isPublish());
		filter.setRead(p.isRead());
		filter.setWrite(p.isWrite());
		filter.setAdmin(p.isAdmin());
		
		return filter;
	}
	
	
	public Permission reload(Permission p) {
		
		 return permissionservice.getPermission(p.getId());
	}
	
	
//	private PermissionFilter permissionFilter(String name, Privilege ...privileges) {
//		
//		DataSetConfiguration set = configuration.dataSet(name);
//		
//		
//	}
}
