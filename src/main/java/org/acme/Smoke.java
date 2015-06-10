package org.acme;

import static org.junit.Assert.*;

import javax.inject.Inject;
import javax.sql.DataSource;

import lombok.SneakyThrows;

import org.acme.utils.SwsTest;
import org.fao.sws.ejb.UserService;
import org.fao.sws.model.cdi.SwsScope;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.fao.sws.model.config.questionnaire.QuestionnairesConfiguration;
import org.fao.sws.model.dao.DimensionDao;
import org.junit.Test;


public class Smoke extends SwsTest {
	
	@Inject @SwsScope
	DataSource source;
	
	@Inject
	UserService users;
	
	
	@Test @SneakyThrows	
	public void can_start_and_inject_resources() {
		
		assertNotNull(source);

	}
	
	@Inject
	DatabaseConfiguration config;
	
	@Test
	public void can_read_config() {
		
		assertNotNull(config);

		assertThatCanBeParsed(config);
	}
	
	@Inject
	QuestionnairesConfiguration qconfig;
	
	@Test
	public void can_load_questionnaires() {
		
		assertNotNull(qconfig);	

		qconfig.load();
		
		assertFalse(qconfig.getQuestionnaires().isEmpty());
	}
	
	
	@Inject
	DimensionDao dao;
	
//	@Test
//	public void can_fire_queries() {
//		
//		config.getDomains().stream().findFirst().ifPresent(d->
//				d.getDataSets().stream().findFirst().ifPresent(ds-> 
//					ds.getDimensions().stream().findFirst().ifPresent(dim-> {
//			  
//					List<DimensionValue> values = dao.getAllDimensions(new DimensionFilter()
//																			 .setDomainCode(d.getCode())
//																			 .setDataSetCode(ds.getCode())
//																			 .setDimensionCode(dim.getCode()));
//					assertFalse(values.isEmpty());
//					;
//
//				})));
//				
//	}
	
}
