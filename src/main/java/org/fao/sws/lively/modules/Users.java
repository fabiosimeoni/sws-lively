package org.fao.sws.lively.modules;

import static java.util.Arrays.*;

import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;

import lombok.experimental.UtilityClass;

import org.fao.sws.domain.operational.Privilege;
import org.fao.sws.domain.plain.operational.Group;
import org.fao.sws.domain.plain.operational.Permission;
import org.fao.sws.domain.plain.operational.User;
import org.fao.sws.ejb.GroupService;
import org.fao.sws.ejb.PermissionService;
import org.fao.sws.ejb.UserService;
import org.fao.sws.lively.LiveTest;
import org.fao.sws.lively.core.SecurityContext;
import org.fao.sws.model.filter.PermissionFilter;

@UtilityClass
public class Users extends Common {
	
	void startup(@Observes LiveTest.Start e, GroupService groups, UserService users, PermissionService permissions) {
		
		Users.groups = groups;
		Users.users=users;
		Users.permissions=permissions;
	}
	
	
	////////////// groups //////////////////////////////////////////////////////////////////////
	
	public GroupService groups;
	
	
	public Predicate<Group> isAdminGroup = g -> g.isAdministrator();
	
	public Predicate<Group> isRegularGroup = isAdminGroup.negate();
	
	
	
	public Stream<Group> groups() {
		
		return groups.getAllGroups(true).stream();
	}
	
	public Stream<Group> adminGroups() {
		
		return groups().filter(isAdminGroup);
	}
	
	public Stream<Group> regularGroups() {
		
		return groups().filter(isRegularGroup);
	}
	
	public Group anAdminGroup() {
		
		return aGroupThat(isAdminGroup);
	}
	
	public Group aRegularGroup() {
		
		return aGroupThat(isRegularGroup);
	}
	
	public Group aGroupThat(Predicate<Group> filter) {
		
		return oneof(groups().filter(filter));
	}
	
	public Stream<Group> groupsOf(User user) {
		return groups.getGroupsByUserId(user.getId()).stream();
	}
	
	
	//////////////// users /////////////////////////////////////////////////////
	
	public UserService users;
	
	public Predicate<User> isAdminUser = u -> groupsOf(u).filter(isAdminGroup).findFirst().isPresent();
	
	public Predicate<User> isRegularUser = isAdminUser.negate();
	
	
	
	public Stream<User> users() {
		
		return users.getAllUsers().stream();

	}
	
	public User aUserThat(Predicate<User> filter) {
		
		return oneof(users().filter(filter));

	}
	
	public Stream<User> admins() {
		
		return users().filter(isAdminUser);
	}
	
	public Stream<User> regularUsers() {
		
		return users().filter(isRegularUser);
	}

	public Stream<User> usersIn(Group group) {
		
		return users.getUsersByGrpId(group.getId()).stream();

	}
	
	public User aUserIn(Group group) {
		
		return oneof(usersIn(group));

	}
	
	public User aUserWith(Permission permission) {
		
		return oneof(usersIn(permission.getGroup()));

	}
	
	public User anAdmin() {
		
		return aUserIn(anAdminGroup()); //faster than oneof(admins());

	}
	
	public User aRegularUser() {
		
		return oneof(regularUsers());

	}
	
	/////// login/out ///////////////////////////////////////////////////////////////////////
	

	SecurityContext sctx;
	
	User currentuser;
	
	public void login(User user) {
		
		log.info("logging {}",user.getUsername());
		
		currentuser = user;
		
		sctx = new SecurityContext(currentuser,groups);
		sctx.bind();
	}
	
	public void logout() {
		
		if (sctx!=null)
			sctx.remove();
	}
	
	public void sudo(User user, Runnable task) {
		
		login(anAdmin());
		
		task.run();
		
		login(user); //relog user
	}
	

	////////////// permissions //////////////////////////////////////////////////////////////////////

	public PermissionService permissions;
	
	public Predicate<Permission> withPrivileges(Privilege ... privileges) {
		return p -> p.getPrivileges().containsAll(asList(privileges));
	}
	
	public Predicate<Permission> withoutPrivileges(Privilege ... privileges) {
		return withPrivileges(privileges).negate();
	}
	
	public Predicate<Permission> over(String name) {
		return p->p.getDataSet().getName().equals(name);
	}
	
	
	
	public Stream<Permission> permissionsOf(Group g) {
		return permissions.getPermissionByGrpId(g.getId()).stream();
	}

	public Stream<Permission> permissionsOf(Stream<Group> groups) {
		
		return 	groups.flatMap(g->permissionsOf(g)).filter(p->!p.isAdmin());
	}
	
	public Stream<Permission> permissionsOver(String datasetname) {
	
		PermissionFilter filter = new PermissionFilter();
		filter.setDataSetCode(datasetname);
		
		return 	permissions.getGroupsPermission(filter).stream();
	}
	
	public void update(Permission permission) {
		
		permissions.update(basedOn(permission));
	}
	
	
	public ToClause assign(Privilege ... privileges) {
		
		return u -> p -> {
			
			sudo(u, ()-> {
				
				p.getPrivileges().addAll(asList(privileges));
				
				update(p);
				
			});
		}; 
	}
	
	public FromClause revoke(Privilege ... privileges) {
		
		return u -> p -> {
			
			sudo(u, ()-> {
				
				p.getPrivileges().removeAll(asList(privileges));
				
				update(p);
				
			});
			
		}; 
	}

	public static interface OverClause {
		
		void over(Permission permission);
	}
	
	public interface ToClause {
		

		OverClause to(User user);
	}
	
	public interface FromClause {
		

		OverClause from(User user);
	}
	
	public PermissionFilter basedOn(Permission p) {
		
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
		
		 return permissions.getPermission(p.getId());
	}
}
