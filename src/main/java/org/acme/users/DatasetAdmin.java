package org.acme.users;

import static org.fao.sws.domain.plain.traits.operational.Privilege.*;
import static org.fao.sws.lively.modules.Configuration.*;
import static org.fao.sws.lively.modules.Scripts.*;
import static org.fao.sws.lively.modules.Users.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.fao.sws.domain.plain.operational.ComputationExecution;
import org.fao.sws.domain.plain.operational.ComputationModule;
import org.fao.sws.domain.plain.operational.Group;
import org.fao.sws.domain.plain.operational.Permission;
import org.fao.sws.domain.plain.operational.User;
import org.fao.sws.domain.plain.reference.DataSet;
import org.fao.sws.domain.r.jaxb.MainDataset;
import org.fao.sws.ejb.ObservationService;
import org.fao.sws.ejb.dto.ComputationModuleDto;
import org.fao.sws.ejb.security.UnauthorizedApplicationException;
import org.fao.sws.lively.SwsTest;
import org.fao.sws.model.config.ConversionTableConfiguration;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.fao.sws.model.dao.ComputationExecutionDao;
import org.fao.sws.model.dao.ComputationScheduledDao;
import org.fao.sws.model.filter.ComputationExecutionFilter;
import org.fao.sws.web.dto.ComputationModulePermissionDto;
import org.fao.sws.web.dto.ConversionTablesDto;
import org.fao.sws.web.dto.PermissionDto;
import org.fao.sws.web.dto.UserDto;
import org.fao.sws.web.rest.ComputationExecutionRest;
import org.fao.sws.web.rest.ComputationModuleRest;
import org.fao.sws.web.rest.ConversionTableRest;
import org.fao.sws.web.rest.GroupRest;
import org.fao.sws.web.rest.UserRest;
import org.junit.Test;

/**
 * SWS:947
 * The system needs a level of user permissions at the administrator level, but restricted to particular datasets. 
 * This could either be a new type/level of user permissions, or an extension to the existing administrator user configuration.
 * https://jira.fao.org/ciok/browse/SWS-947
 */
public class DatasetAdmin extends SwsTest {
	
	@Inject
	UserRest userapi;
	
	@Inject
	ConversionTableRest tableapi;
	
	@Inject
	DatabaseConfiguration config;
	
	
	///////////////// basic support   //////////////////////////////////////////////////////////////////////
	
	@Test
	public void add_read_and_verify_admin_permission_for_group() {
		
		User user = aNewUser();
		DataSet dataset = aDataset();
		
		assertFalse(groupservice.adminsDataset(groupOf(user),dataset));
		assertFalse(userservice.adminsDataset(user,dataset));
		
		assign(ADMIN).to(groupOf(user)).over(dataset);
		
		assertTrue(groupservice.adminsDataset(groupOf(user),dataset));
		assertTrue(userservice.adminsDataset(user,dataset));
	}
	
	
	@Test
	public void admin_permission_implies_any_other_permission() {
		
		Permission permission = assign(WRITE).to(aNewGroup()).over(aDataset());
		
		assertFalse(permission.isRead());
		
		permission = add(ADMIN).to(permission);
				
		assertTrue(permission.isRead());
	}
	
	
	/////////////////// DAs and user management ////////////////////////////////////////////////////////
	
	
	@Test
	public void DAs_can_change_permissions_to_their_datasets() {

		User user = aNewUser();
		
		Permission permission = assign(READ).to(groupOf(user)).over(aDataset());
		
		login(user);
		
		try {
			
			 //low-level up that requires admin privileges
			 permissionservice.update(with(permission));

			fail();
		}
		catch(UnauthorizedApplicationException expected) {}
		
		sudo(() -> 
		
				add(ADMIN).to(permission)
		);
		
		permissionservice.update(with(permission)); //no problem now

	}
	
	
	@Test
	public void DAs_can_assign_new_users_to_their_datasets() {

		User user = aNewUser();
		User anotherUser = aNewUser();	
		DataSet dataset = aDataset();

		login(user);
		
		try {
		
			assign(READ).to(groupOf(anotherUser)).over(dataset);
			
			fail();
		}
		catch(UnauthorizedApplicationException expected) {}
		
		
		//promote to DA
		
		sudo(
				()->assign(ADMIN).to(groupOf(user)).over(dataset)
		);
		
		
		assign(READ).to(groupOf(anotherUser)).over(dataset);
		
		
		//still not on other datasets
		
		DataSet anotherDataset = aDatasetThat(isOtherThan(dataset));
		
		try {
			
			assign(READ).to(groupOf(anotherUser)).over(anotherDataset);
			
			fail();
		}
		catch(UnauthorizedApplicationException expected) {}

	}

	@Test
	public void DAs_optionally_see_a_subset_of_users() {
	
		User caller = aNewUser();
		User user1 = aNewUser();
		User user2 = aNewUser();
		
		login(caller);
		
		List<UserDto> results = userapi.getAllUsers(false).getResults(); //no permissions
		
		assertTrue(includes(results, user1));
		assertTrue(includes(results, user2));
		
		results = userapi.getAllUsers(true).getResults();  //with permissions, without privileges
		
		assertFalse(includes(results,user1));
		assertFalse(includes(results,user2));
		
		DataSet dataset = aDataset();
		
		
		sudo(() -> //promote caller to DA
		
			assign(ADMIN).to(groupOf(caller)).over(dataset)
		);
		
		//assign user one to work on dataset
		assign(READ).to(groupOf(user1)).over(dataset);	
		
		results = userapi.getAllUsers(true).getResults();
		

		assertTrue(includes(results,user1));
		assertFalse(includes(results,user2));
		
	}
	
	
	@Inject
	GroupRest groupapi;
	
	@Test
	public void DAs_see_a_subset_of_group_permissions() {
		
		User user = aNewUser();
		Group group = groupOf(user);
		DataSet dataset = aDataset();
		
		Permission permission = assign(READ).to(group).over(dataset);
	
		List<PermissionDto> results = groupapi.getPermissionByGrpId(group.getId()).getResults();
		
		assertTrue(includes(results, permission));
		
		login(user);
		
		results = groupapi.getPermissionByGrpId(group.getId()).getResults();
		
		assertTrue(results.isEmpty());
		
		sudo(() -> 
		
			add(ADMIN).to(permission)
			
		);
	
		results = groupapi.getPermissionByGrpId(group.getId()).getResults();
		
		assertTrue(includes(results, permission));
		
	}
	
	
	/////////////////// conversion tables ////////////////////////////////////////////////////////
	
	@Test
	public void dataset_admins_can_see_conversion_tables_defined_over_their_datasets() {
		
		List<ConversionTablesDto> tables = tableapi.getConversionTables().getResults();
		
		assertFalse(tables.isEmpty());
		
		ConversionTablesDto dto = tables.get(0);
		
		ConversionTableConfiguration table = config.getConversionTable(dto.getDomainCode(), dto.getDataSetCode(),dto.getConversionTableCode());
		
		Permission permission = aRegularPermissionOfaRegularGroupOver(table.sourceDataset().getCode());
		
		User user = aUserIn(permission.getGroup());
		
		login(user);
		
		tables = tableapi.getConversionTables().getResults();
		
		assertTrue(tables.isEmpty());
		
		add(ADMIN).to(permission);
		
		//admin over source is NOT enough
		
		tables = tableapi.getConversionTables().getResults();
		
		assertTrue(tables.isEmpty());
		
		//admin over source and target IS enough
		
		permission = aRegularPermissionOfaRegularGroupOver(table.targetDataset().getCode());
		
		add(ADMIN).to(permission);
		
		tables = tableapi.getConversionTables().getResults();
		
		assertFalse(tables.isEmpty());
	}

	
	///////////////  R-modules     ///////////////////////////////////////////////////////////////////////////////////
	
	
	@Inject
	ComputationModuleRest moduleService;
	

	@Inject
	ComputationExecutionDao computations;
	
	
	@Inject
	ComputationScheduledDao scheduledcomputations;
	
	/**
	 * @see ComputationModuleRest#getComputationModules(Boolean)
	 */
	@Test
	public void dataset_admins_can_see_modules_defined_over_their_datasets() {

		ComputationModule module = aModule();
		
		MainDataset maindataset = module.getMetadata().getDatasets().getMainDatasetsList().get(0);
		
		Permission permission = aRegularPermissionOfaRegularGroupOver(maindataset.getCode());
		
		User user = aUserIn(permission.getGroup());

		assertFalse(module.getOwner().getId() == user.getId()); //test sanity check
		
		login(user);
		
		assertTrue(moduleService.getComputationModules(true).getResults().isEmpty());
		
		add(ADMIN).to(permission);
		
		moduleService.getComputationModules(true)
									.getResults()
									.stream()
									.peek(g->log.info("category: {}",g.getCategory()))
									.flatMap(g->g.getComputations().stream())
									.forEach(m->log.info("--module: {}, owner {}",m.getCode(),m.getOwnerName()));
		
		Optional<ComputationModuleDto> dtoForInitialModule = moduleService.getComputationModules(true)
																	.getResults().stream()
																	.flatMap(g->g.getComputations().stream())
																	.filter(m->m.getUid().equals(module.getId()))
																	.findFirst();
		
		assertTrue(dtoForInitialModule.isPresent());
		
		
		
	}
	
	
	@Inject
	ComputationExecutionRest executions;
	
	/**
	 * @see ComputationExecutionRest#list(Long, Long, Boolean, Long, org.fao.sws.domain.plain.operational.ComputationExecution.Status, Long)
	 */
	@Test
	public void dataset_admins_can_see_executions_defined_over_their_datasets() {
		
		User user = userservice.getUser("faodomain/simeoni");
		
		login(user);
		
		executions.list(null,null,false,null,null,null).getResults();
		
		
	}
	
	@Inject
	public ConversionTableRest tableService;
	
	@Test
	public void download_module() {
		
		User user = userservice.getUser("faodomain/simeoni");
		
		login(user);
		
		moduleService.downloadComputationModuleByNameAndVersion("BugfixTest", 2);
		
		
	}
	
	
	@Test
	public void dataset_admins_can_see_their_conversion_tables() {
		
		User user = userservice.getUser("faodomain/simeoni");
		
		login(user);
		
		List<ConversionTablesDto> dtos = tableService.getAllConversionTablesCodes().getResults();
		
		show(all(dtos),t->t.getConversionTableCode());
		
		
	}
	
	@Test
	public void dataset_admins_can_see_permissions_of_modules_defined_over_their_datasets() {
	
		ComputationModule module = aModule();
		
		MainDataset maindataset = module.getMetadata().getDatasets().getMainDatasetsList().get(0);
		
		Permission permission = aRegularPermissionOfaRegularGroupOver(maindataset.getCode());
		
		User user = aUserIn(permission.getGroup());
		
		login(user);
		
		List<ComputationModulePermissionDto> permissions = moduleService.getPermissions(module.getId()).getResults();
		
		show(all(permissions),p->p.getId());
		
		assertTrue(permissions.isEmpty());
		
		add(ADMIN).to(permission);
		
		permissions = moduleService.getPermissions(module.getId()).getResults();
		
		assertFalse(permissions.isEmpty());
	}
	
	
	
	@Test
	public void dataset_admins_can_see_scheduled_executions_defined_over_their_datasets() {

		ComputationModule module = aModule();
		
		schedule(module);
		
		boolean hasBeenScheduled = !scheduledcomputations.findAll().isEmpty();
		
		assertTrue(hasBeenScheduled);
		
		MainDataset maindataset = module.getMetadata().getDatasets().getMainDatasetsList().get(0);
		
		Permission permission = aRegularPermissionOfaRegularGroupOver(maindataset.getCode());
		
		User user = aUserIn(permission.getGroup());
		
		login(user);
		
		assertTrue(scheduledcomputations.findAllComputationScheduledPermitted().isEmpty());
		
		add(ADMIN).to(permission);
		
		assertFalse(scheduledcomputations.findAllComputationScheduledPermitted().isEmpty());
		
	}
	
	
	@Test
	public void dataset_admins_can_only_see_history_of_modules_defined_over_their_datasets() {

		ComputationModule module = aModule();
		
		List<ComputationModuleDto> history = moduleService.getComputationModules(module.getName(),module.isCoreScript()).getResults();
		
		show(all(history), c->c.getStart());
		
		assertFalse(history.isEmpty());
		
		Permission permission = aRegularPermissionOfaRegularGroupOver(mainDatasetOf(module));
		
		User user = aUserIn(permission.getGroup());
		
		login(user);
		
		history = moduleService.getComputationModules(module.getName(),module.isCoreScript()).getResults();
		
		assertTrue(history.isEmpty());
		
		add(ADMIN).to(permission);
		
		history = moduleService.getComputationModules(aModule().getName(),aModule().isCoreScript()).getResults();
		
		assertFalse(history.isEmpty());
	}
	@Inject
	ObservationService observations;
	
	@Test //this is massively slow	
	public void dataset_admins_can_rollback_executions_of_modules_defined_over_their_datasets() {
		
		ComputationModule module = aModule();
		
		ComputationExecutionFilter filter = new ComputationExecutionFilter().setComputationModuleId(module.getId());
		
		ComputationExecution execution = computations.search(filter).get(0);
		
		MainDataset maindataset = module.getMetadata().getDatasets().getMainDatasetsList().get(0);
		
		Permission permission = aRegularPermissionOfaRegularGroupOver(maindataset.getCode());
		
		User user = aUserIn(permission.getGroup());
		
		login(user);
		
		try {
			observations.rollbackBeforeComputationExecution(execution, true);
			fail();
		}
		catch(UnauthorizedApplicationException gottahappen){}
		
		add(ADMIN).to(permission);
		
		observations.rollbackBeforeComputationExecution(execution, true);
	}
	
	
	///// helpers ////////////////////////////////////////////////////////////////////////////////////////////
	
	public Permission aRegularPermissionOfaRegularGroup() {
		
		return oneof(permissionsOf(regularGroups()).filter(withoutPrivileges(ADMIN)));
	}
	
	public Permission aRegularPermissionOfaRegularGroupOver(String dataset) {
		
		return oneof(permissionsOf(regularGroups()).filter(withoutPrivileges(ADMIN).and(over(dataset))));
		
	}
	
	
	private boolean includes(List<UserDto> users,User user) {
		return all(users.stream().map(dto->dto.getId())).contains(user.getId());
	}
	
	private boolean includes(List<PermissionDto> permissions,Permission permission) {
		return all(permissions.stream().map(dto->dto.getId())).contains(permission.getId());
	}
	
	
	
}
