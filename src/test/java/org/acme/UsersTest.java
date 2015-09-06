package org.acme;

import static org.fao.sws.domain.plain.traits.operational.Privilege.*;
import static org.fao.sws.lively.modules.Configuration.*;
import static org.fao.sws.lively.modules.Users.*;
import static org.junit.Assert.*;

import org.fao.sws.domain.plain.operational.Group;
import org.fao.sws.domain.plain.operational.Permission;
import org.fao.sws.domain.plain.operational.User;
import org.fao.sws.lively.SwsTest;
import org.junit.Test;

public class UsersTest extends SwsTest {

	@Test
	public void group_support() {
		
		log.info("groups:{}",groups().count());
		log.info("admins:{}",adminGroups().count());
		log.info("regulars:{}",regularGroups().count());
		log.info("monouser:{}",allGroupsThat(g->g.isSingleUser()).count());
		
		Group group = aNewGroup();
		
		group = group(group.getId());  
		group = group(group.getName()); 
			
	}
	
	@Test
	public void user_support() {
		
		log.info("users:{}",users().count());
		log.info("admins:{}",admins().count());
		log.info("regulars:{}",regularUsers().count());
		log.info("inactive:{}",allUsersThat(u->!u.isActive()).count());
		
		
		User user = aNewUser();

		assertEquals(user,user(user.getUsername()));
		
		Group group = oneof(all(user.getGroups()));
		
		user = aNewUserIn(group);
		
		join(aNewUser()).to(group);
		
		assertTrue(usersIn(group).count()==3);
		
	}
	
	@Test
	public void permission_support() {
		
		Permission p = assign(ADMIN).to(groupOf(aNewUser())).over(aDataset());
		
		assertEquals(p,permission(p.getId()));

		log.info("dataset:{}",p.getDataSet().getName());
		log.info("group:{}",p.getGroup().getName());
		log.info("scope:{}",p.getDimension2ids());
		log.info("rights:{}",p.getPrivileges());

		

	}
}
