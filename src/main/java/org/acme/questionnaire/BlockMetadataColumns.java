package org.acme.questionnaire;

import static org.junit.Assert.*;

import java.io.File;

import javax.inject.Inject;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;

import org.fao.sws.ejb.DimensionService;
import org.fao.sws.ejb.QuestionnairesService;
import org.fao.sws.lively.SwsTest;
import org.fao.sws.lively.core.Extensions;
import org.fao.sws.model.config.questionnaire.QuestionnaireConfiguration;
import org.fao.sws.model.config.questionnaire.QuestionnairesConfiguration;
import org.fao.sws.model.dao.BlockMetadataDao;
import org.fao.sws.model.dao.impl.mapping.BlockMetadataQuestionnaireDto;
import org.fao.sws.model.dao.impl.mapping.DataProvider;
import org.fao.sws.model.dto.QuestionnaireExportDto;
import org.fao.sws.model.filter.DimensionFilter;
import org.fao.sws.web.rest.QuestionnaireRest;
import org.junit.Before;
import org.junit.Test;

@ExtensionMethod(Extensions.class)
public class BlockMetadataColumns extends SwsTest {

	QuestionnairesConfiguration questionnaires = new QuestionnairesConfiguration();
	QuestionnaireConfiguration questionnaire;
	
	String codeforFrance = "250";
	long keyforFrance;
	String codefor2013 = "2013";
	long keyfor2013;
	
	@Inject
	DimensionService dims;
	
	@Inject
	BlockMetadataDao dao;
	
	@Inject
	QuestionnairesService ejb;
	
	@Inject
	QuestionnaireRest service;

	
	@Before 
	public void setup() {
		
		
		questionnaires.load();
		
		questionnaire = questionnaires.getQuestionnaire("ap-main");
		
		DimensionFilter M49inAgriculture = new DimensionFilter()
														.setDomainCode("agriculture")
														.setDataSetCode("agriculture")
														.setDimensionCode("geographicAreaM49");
		
		keyforFrance = dims.getDimensionValue(M49inAgriculture.setDimensionValueCode(codeforFrance)).getId();
		
		DimensionFilter yearsInAgriculture = new DimensionFilter()
														.setDomainCode("agriculture")
														.setDataSetCode("agriculture")
														.setDimensionCode("timePointYears");
		
		keyfor2013 = dims.getDimensionValue(yearsInAgriculture.setDimensionValueCode(codefor2013)).getId();
	}

	
	
	@Test @SneakyThrows 
	public void query_can_be_executed() {
		
		@Cleanup DataProvider<BlockMetadataQuestionnaireDto> result = dao.getBlockMetadataProvider(questionnaire, keyforFrance, keyfor2013);

		assertFalse(result.isEmpty());

		BlockMetadataQuestionnaireDto dto = null;
		
		while ((dto=result.next())!=null)
			System.out.println(dto.value());

	}
	
	@Test
	public void ejb_creates_correct_questionnaire() {
	
		File file = new File("target/q.xls");
		
		ejb.createQuestionnaire(questionnaire, 
								questionnaires.getMetadataConfigurations(), 
								codefor2013, 
								codeforFrance, 
								file);
		
	}
	
	@Test @SneakyThrows
	public void service_generates_questionnaire() {
	
		QuestionnaireExportDto dto = new QuestionnaireExportDto(questionnaire.getName(), codefor2013, codeforFrance);
		
		dto.setDownloadBaseUrl("target");
		
		service.startQuestionnaireExport(dto);
		
		Thread.sleep(10000);
		
	}

}
