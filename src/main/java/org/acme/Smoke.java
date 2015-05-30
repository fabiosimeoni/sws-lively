package org.acme;

import static org.junit.Assert.*;

import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.acme.utils.OocTest;
import org.fao.sws.domain.plain.reference.DimensionValue;
import org.fao.sws.model.cdi.SwsScope;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.fao.sws.model.dao.DimensionDao;
import org.fao.sws.model.filter.DimensionFilter;
import org.junit.Test;

public class Smoke extends OocTest {
	
	@Inject @SwsScope
	DataSource source;
	
	@Inject
	DatabaseConfiguration config;
	
	@Test
	public void can_inject_resources() {
		
		assertNotNull(source);
		assertNotNull(config);
		
	}
	
	@Test
	public void can_read_config() {
		
		assertCanBeParsed(config);
	}
	
	
	@Inject
	DimensionDao dao;
	
	@Test
	public void can_fire_queries() {
		
		config.getDomains().stream().findFirst().ifPresent(d->
				d.getDataSets().stream().findFirst().ifPresent(ds-> 
					ds.getDimensions().stream().findFirst().ifPresent(dim-> {
			  
					List<DimensionValue> values = dao.getAllDimensions(new DimensionFilter()
																			 .setDomainCode(d.getCode())
																			 .setDataSetCode(ds.getCode())
																			 .setDimensionCode(dim.getCode()));
					assertFalse(values.isEmpty());

				})));
				
	}
	
}
