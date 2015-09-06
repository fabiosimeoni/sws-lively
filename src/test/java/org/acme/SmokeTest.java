package org.acme;

import static org.junit.Assert.*;

import javax.inject.Inject;

import org.fao.sws.lively.SwsTest;
import org.fao.sws.model.dao.DataSetDao;
import org.junit.Test;

public class SmokeTest extends SwsTest {

	@Inject
	DataSetDao datasets;
	
	@Test
	public void container_starts() {}
	

	@Test
	public void test_is_injected() {
		
		assertNotNull(datasets);
	}
	
	@Test
	public void can_access_the_db() {
		
		assertNotNull(datasets.getCompleteListOfDatasetsFromDb());
	
	}
	
}
