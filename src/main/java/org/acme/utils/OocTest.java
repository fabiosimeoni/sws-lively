package org.acme.utils;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import javax.inject.Inject;

import org.fao.sws.domain.plain.operational.User;
import org.fao.sws.ejb.UserService;
import org.fao.sws.ejb.security.SecurityContextCreator;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.googlecode.jeeunit.JeeunitRunner;

@RunWith(JeeunitRunner.class)
public abstract class OocTest {

	public static Logger log = Logger.getLogger("test");
	
	@Inject
	UserService users;
	
	protected User user;

	@BeforeClass
	public static void $suitesetup() {
		
		SLF4JBridgeHandler.install();
		
		System.setProperty(DatabaseConfiguration.CONFIG_ROOT_PROPERTY, "src/main/resources/config");
		
	}
	
	@Before public void $testsetup() {
		
		user = users.getUser("FAODOMAIN/browningj");
		
		new SecurityContextCreator(user,false).bind();
		
	}
	
	
	public void assertCanBeParsed(DatabaseConfiguration config) {
		
		assertNull(config.consumeConfigErrors());
		assertNull(config.consumeConfigAlerts());
	}
	

}
