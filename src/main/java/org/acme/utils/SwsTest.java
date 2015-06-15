package org.acme.utils;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.sql.DataSource;

import lombok.SneakyThrows;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.fao.sws.domain.plain.operational.User;
import org.fao.sws.ejb.UserService;
import org.fao.sws.ejb.security.SecurityContextCreator;
import org.fao.sws.model.cdi.SwsScope;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;


@RunWith(CdiTestRunner.class)
public abstract class SwsTest {
	
	public static Logger log = Logger.getLogger("test");
	
	@Inject
	UserService users;

	@Inject @SwsScope
	DataSource source;
	
	protected User user;
	
	@BeforeClass
	public static void $suitesetup() {
		
		log.info("cdi container is up");
		 
		System.setProperty(DatabaseConfiguration.CONFIG_ROOT_PROPERTY, "src/main/resources");		
	}
	
	@Before public void $testsetup() {
		
		//a super-user
		user = users.getUser("FAODOMAIN/browningj");
		
		new SecurityContextCreator(user,false).bind();
	}
	
	@SneakyThrows @After public void $testteardown() {
		
		source.getConnection().rollback();
	}
    
    
	//////////////////////////////////////////////////////////////////////////
	
	public void assertThatCanBeParsed(DatabaseConfiguration config) {
		
		assertNull(config.consumeConfigErrors());
		assertNull(config.consumeConfigAlerts());
	}
	

}
