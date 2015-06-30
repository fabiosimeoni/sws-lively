package org.acme.security;

import static org.acme.ooc.Common.*;
import static org.acme.ooc.Modules.*;
import static org.acme.ooc.Users.*;
import static org.fao.sws.domain.operational.Privilege.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.acme.ooc.LiveTest;
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
import org.fao.sws.model.config.ConversionTableConfiguration;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.fao.sws.model.dao.ComputationExecutionDao;
import org.fao.sws.model.dao.ComputationScheduledDao;
import org.fao.sws.model.filter.ComputationExecutionFilter;
import org.fao.sws.model.filter.PermissionFilter;
import org.fao.sws.web.dto.ComputationModulePermissionDto;
import org.fao.sws.web.dto.ConversionTablesDto;
import org.fao.sws.web.dto.PermissionDto;
import org.fao.sws.web.dto.UserDto;
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
public class DatasetAdmin extends LiveTest {
	
	@Inject
	UserRest userservice;
	
	@Inject
	ConversionTableRest tableservice;
	
	@Inject
	DatabaseConfiguration config;
	
	@Test
	public void add_read_and_verify_admin_permission_for_group() {
		
		Permission permission = aRegularPermissionOfaRegularGroup();
		
		permission.getPrivileges().add(ADMIN);
		
		update(permission);
		
		permission = reload(permission);
		
		assertTrue(groups.adminsDataset(permission.getGroup(),permission.getDataSet()));
	}
	
		
	@Test
	public void verify_admin_permission_for_user() {
		
		Permission permission = aRegularPermissionOfaRegularGroup();
		
		DataSet dataset = permission.getDataSet();
		
		User user = aUserIn(permission.getGroup());
		
		assertFalse(users.adminsDataset(user,dataset));
		
		permission.getPrivileges().add(ADMIN);
		
		update(permission);
		
		assertTrue(users.adminsDataset(user,dataset));
	}
	
	
	@Test
	public void admin_permission_implies_any_other_permission() {
		
		Permission permission = aRegularPermissionOfaRegularGroup();
		
		permission.getPrivileges().remove(READ);
		
		update(permission);
		
		permission = reload(permission);
		
		assertFalse(permission.isRead());
		
		permission.getPrivileges().add(ADMIN);
		
		update(permission);
		
		permission = reload(permission);
		
		assertTrue(permission.isRead());
	}
	
	
	@Test
	public void dataset_admins_can_change_permissions_to_their_datasets() {

		Permission permission = aRegularPermissionOfaRegularGroup();
		
		User user = aUserIn(permission.getGroup());
		
		login(user);
		
		try {
			
			update(permission);

			fail();
		}
		catch(UnauthorizedApplicationException expected) {}
		
		assign(ADMIN).to(user).over(permission);
		
		update(permission); //no problem now

	}
	
	@Test
	public void dataset_admins_can_see_conversion_tables_defined_over_their_datasets() {
		
		List<ConversionTablesDto> tables = tableservice.getConversionTables().getResults();
		
		assertFalse(tables.isEmpty());
		
		ConversionTablesDto dto = tables.get(0);
		
		ConversionTableConfiguration table = config.getConversionTable(dto.getDomainCode(), dto.getDataSetCode(),dto.getConversionTableCode());
		
		Permission permission = aRegularPermissionOfaRegularGroupOver(table.sourceDataset().getCode());
		
		User user = aUserIn(permission.getGroup());
		
		login(user);
		
		tables = tableservice.getConversionTables().getResults();
		
		assertTrue(tables.isEmpty());
		
		assign(ADMIN).to(user).over(permission);
		
		//admin over source is NOT enough
		
		tables = tableservice.getConversionTables().getResults();
		
		assertTrue(tables.isEmpty());
		
		//admin over source and target IS enough
		
		permission = aRegularPermissionOfaRegularGroupOver(table.targetDataset().getCode());
		
		assign(ADMIN).to(user).over(permission);
		
		tables = tableservice.getConversionTables().getResults();
		
		assertFalse(tables.isEmpty());
	}
	
	
	@Test
	public void dataset_admins_can_assign_new_users_to_their_datasets() {

		Permission permission = aRegularPermissionOfaRegularGroup();
		
		User user = aUserIn(permission.getGroup());

		login(user);

		assign(ADMIN).to(user).over(permission);
		
		PermissionFilter filter = basedOn(permission);
		
		filter.setSessionDescription("let's change the description to pass duplication checks");
		
		permissions.create(filter);

		
	}

	@Test
	public void dataset_admins_optionally_see_a_subset_of_users() {
	
		Permission permission = aRegularPermissionOfaRegularGroup();
		
		Group group = permission.getGroup();
		
		User user = aUserIn(group);
		
		login(user);
		
		List<UserDto> users = userservice.getAllUsers(false).getResults(); //no permissions
		
		assertFalse(users.isEmpty());
		
		users = userservice.getAllUsers(true).getResults();  //with permissions, without privileges
		
		assertTrue(users.isEmpty());
		
		assign(ADMIN).to(user).over(permission);
		
		users = userservice.getAllUsers(true).getResults();
		
		assertFalse(users.isEmpty()); //with permissions and privileges
		
		show(users,u->u.getUsername());
		
		//more than other live tests, this is an approximate test, which depends on db state.
		//maing precise assertions against that state would required duplicating implementation logic.
 	
	}
	
	
	@Inject
	GroupRest groupservice;
	
	@Test
	public void dataset_admins_see_a_subset_of_a_group_permissions() {
	
		Permission permission = aRegularPermissionOfaRegularGroup();
		
		Group group = permission.getGroup();
		
		List<PermissionDto> perms = groupservice.getPermissionByGrpId(group.getId()).getResults();
		
		assertFalse(perms.isEmpty());
		
		User user = aUserIn(group);
		
		login(user);
		
		perms = groupservice.getPermissionByGrpId(group.getId()).getResults();
		
		assertTrue(perms.isEmpty());
		
		assign(ADMIN).to(user).over(permission);
		
		perms = groupservice.getPermissionByGrpId(group.getId()).getResults();
		
		assertFalse(perms.isEmpty());
		
		Optional<PermissionDto> dtoForInitialPermission =  perms.stream().filter(p->p.getId().equals(permission.getId())).findFirst();
				
		assertTrue(dtoForInitialPermission.isPresent());
		
		assertTrue(dtoForInitialPermission.get().isAdmin());
		
	}

	
	///////////////  R-modules     ///////////////////////////////////////////////////////////////////////////////////
	
	
	@Inject
	ComputationModuleRest modules;
	

	@Inject
	ComputationExecutionDao computations;
	
	
	@Inject
	ComputationScheduledDao scheduledcomputations;
	
	
	
	/**
	 * @see ComputationModuleRest#getComputationModules(Boolean)
	 */
	@Test
	public void dataset_admins_can_only_see_modules_defined_over_their_datasets() {

		ComputationModule module = aModule();
		
		MainDataset maindataset = module.getMetadata().getDatasets().getMainDatasetsList().get(0);
		
		Permission permission = aRegularPermissionOfaRegularGroupOver(maindataset.getCode());
		
		User user = aUserIn(permission.getGroup());

		assertFalse(module.getOwner().getId() == user.getId()); //test sanity check
		
		login(user);
		
		assertTrue(modules.getComputationModules(true).getResults().isEmpty());
		
		assign(ADMIN).to(user).over(permission);
		
		modules.getComputationModules(true)
									.getResults()
									.stream()
									.peek(g->log.info("category: {}",g.getCategory()))
									.flatMap(g->g.getComputations().stream())
									.forEach(m->log.info("--module: {}, owner {}",m.getCode(),m.getOwnerName()));
		
		Optional<ComputationModuleDto> dtoForInitialModule = modules.getComputationModules(true)
																	.getResults().stream()
																	.flatMap(g->g.getComputations().stream())
																	.filter(m->m.getUid().equals(module.getId()))
																	.findFirst();
		
		assertTrue(dtoForInitialModule.isPresent());
		
		
		
	}
	
	@Test
	public void dataset_admins_can_only_see_permissions_of_modules_defined_over_their_datasets() {
	
		ComputationModule module = aModule();
		
		MainDataset maindataset = module.getMetadata().getDatasets().getMainDatasetsList().get(0);
		
		Permission permission = aRegularPermissionOfaRegularGroupOver(maindataset.getCode());
		
		User user = aUserIn(permission.getGroup());
		
		login(user);
		
		List<ComputationModulePermissionDto> permissions = modules.getPermissions(module.getId()).getResults();
		
		show(permissions,p->p.getId());
		
		assertTrue(permissions.isEmpty());
		
		assign(ADMIN).to(user).over(permission);
		
		permissions = modules.getPermissions(module.getId()).getResults();
		
		assertFalse(permissions.isEmpty());
	}
	
	
	
	@Test
	public void dataset_admins_can_only_see_computations_defined_over_their_datasets() {

		ComputationModule module = aModule();
		
		schedule(module);
		
		boolean hasBeenScheduled = !scheduledcomputations.findAll().isEmpty();
		
		assertTrue(hasBeenScheduled);
		
		MainDataset maindataset = module.getMetadata().getDatasets().getMainDatasetsList().get(0);
		
		Permission permission = aRegularPermissionOfaRegularGroupOver(maindataset.getCode());
		
		User user = aUserIn(permission.getGroup());
		
		login(user);
		
		assertTrue(scheduledcomputations.findAllComputationScheduledPermitted().isEmpty());
		
		assign(ADMIN).to(user).over(permission);
		
		assertFalse(scheduledcomputations.findAllComputationScheduledPermitted().isEmpty());
		
	}
	
	
	@Test
	public void dataset_admins_can_only_see_history_of_computations_defined_over_their_datasets() {

		ComputationModule module = aModule();
		
		List<ComputationModuleDto> history = modules.getComputationModules(module.getName(),module.isCoreScript()).getResults();
		
		show(history, c->c.getStart());
		
		assertFalse(history.isEmpty());
		
		Permission permission = aRegularPermissionOfaRegularGroupOver(mainDatasetOf(module));
		
		User user = aUserIn(permission.getGroup());
		
		login(user);
		
		history = modules.getComputationModules(module.getName(),module.isCoreScript()).getResults();
		
		assertTrue(history.isEmpty());
		
		assign(ADMIN).to(user).over(permission);
		
		history = modules.getComputationModules(aModule().getName(),aModule().isCoreScript()).getResults();
		
		assertFalse(history.isEmpty());
	}
	
	
	@Test
	public void dataset_admins_can_only_see_scheduled_computations_defined_over_their_datasets() {

		ComputationModule module = aModule();
		
		schedule(module);
		
		boolean hasBeenScheduled = !scheduledcomputations.findAll().isEmpty();
		
		assertTrue(hasBeenScheduled);
		
		Permission permission = aRegularPermissionOfaRegularGroupOver(mainDatasetOf(module));
		
		User user = aUserIn(permission.getGroup());
		
		login(user);
		
		assertTrue(scheduledcomputations.findAllComputationScheduledPermitted().isEmpty());
		
		assign(ADMIN).to(user).over(permission);
		
		assertFalse(scheduledcomputations.findAllComputationScheduledPermitted().isEmpty());
		
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
		
		assign(ADMIN).to(user).over(permission);
		
		observations.rollbackBeforeComputationExecution(execution, true);
	}
	
	
	///// helpers ////////////////////////////////////////////////////////////////////////////////////////////
	
	public Permission aRegularPermissionOfaRegularGroup() {
		
		return oneof(permissionsOf(regularGroups()).filter(withoutPrivileges(ADMIN)));
	}
	
	public Permission aRegularPermissionOfaRegularGroupOver(String dataset) {
		
		return oneof(permissionsOf(regularGroups()).filter(withoutPrivileges(ADMIN).and(over(dataset))));
		
	}
	
	

	
}
