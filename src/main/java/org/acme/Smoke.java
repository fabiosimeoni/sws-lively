package org.acme;

import static org.junit.Assert.*;

import java.util.List;

import javax.inject.Inject;

import lombok.SneakyThrows;

import org.acme.utils.LiveTest;
import org.fao.sws.domain.plain.reference.DimensionValue;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.fao.sws.model.config.questionnaire.QuestionnairesConfiguration;
import org.fao.sws.model.dao.DimensionDao;
import org.fao.sws.model.filter.DimensionFilter;
import org.junit.Test;

public class Smoke extends LiveTest {

	@Inject
	DatabaseConfiguration config;
	
	@Test
	public void configuration_is_available() {
		
		assertNotNull(config);
		
		assertNull(config.consumeConfigAlerts());
		assertNull(config.consumeConfigErrors());
		
	}
	
	
	@Inject
	QuestionnairesConfiguration qconfig;
	
	
	@Test
	public void questionnaire_configuration_is_available() {
		
		assertNotNull(qconfig);
		
		assertNull(qconfig.consumeConfigAlerts());
		assertNull(qconfig.consumeConfigErrors());
				
	}
	
	
	
	@Inject
	DimensionDao dao;
	
	@Test @SneakyThrows
	public void queries_can_be_fired() {
		
		DimensionFilter filter = new DimensionFilter().setDomainCode("agriculture")
													  .setDataSetCode("agriculture")
													  .setDimensionCode("measuredItemCPC");
		
		List<DimensionValue> results = dao.getAllDimensions(filter);
		
		results.forEach(r->System.out.println(r.getCode()));
		
		
	}
}
