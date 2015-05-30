package org.acme.utils;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.fao.sws.model.config.DatabaseConfiguration;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.googlecode.jeeunit.JeeunitRunner;

@RunWith(JeeunitRunner.class)
public abstract class OocTest {

	public static Logger log = Logger.getLogger("test");

	@BeforeClass
	public static void $setup() {
		
		SLF4JBridgeHandler.install();
		
		System.setProperty(DatabaseConfiguration.CONFIG_ROOT_PROPERTY, "src/main/resources/config");
		
	}
	
	
	
	
	
	public void assertCanBeParsed(DatabaseConfiguration config) {
		
		assertNull(config.consumeConfigErrors());
		assertNull(config.consumeConfigAlerts());
	}

}
