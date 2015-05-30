package org.acme;

import static org.junit.Assert.*;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.acme.utils.OocTest;
import org.fao.sws.model.cdi.SwsScope;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.junit.Test;

public class SmokeTest extends OocTest {
	
	@Inject @SwsScope
	DataSource source;
	
	@Inject
	DatabaseConfiguration config;
	
	@Test
	public void can_inject_resources() {
		
		assertNotNull(source);
		assertNotNull(config);
		
		assertCanBeParsed(config);
	}
	
}
