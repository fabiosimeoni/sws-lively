package org.acme;

import static org.junit.Assert.*;

import java.io.File;

import javax.inject.Inject;

import org.fao.sws.ejb.ConfigBootstrapService;
import org.fao.sws.lively.SwsTest;
import org.fao.sws.model.config.DataSetConfiguration;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.fao.sws.web.rest.EditingSessionRest;
import org.junit.Test;

public class CliTest extends SwsTest {

	@Inject
	ConfigBootstrapService service;
	
	@Inject
	DatabaseConfiguration config;
	
	@Inject
	EditingSessionRest rest;
	
	@Test
	public void poortest() {
		
		DataSetConfiguration ds = config.getDataSetConfiguration("agriculture");

		assertNotNull(ds);
		
		int result = service.reloadConfigFile(new File("src/main/resources/config/dataset/C030Agriculture.xml"));
		
		assertTrue(result==0);
		
		ds = config.getDataSetConfiguration("agriculture");
		
		assertNotNull(ds);
		

	}
}
