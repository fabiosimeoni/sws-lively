package org.acme.questionnaire;

import static org.fao.sws.model.configuration.Dsl.*;
import static org.junit.Assert.*;

import java.io.File;

import javax.inject.Inject;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;

import org.acme.utils.Extensions;
import org.acme.utils.SwsTest;
import org.fao.sws.ejb.DimensionService;
import org.fao.sws.ejb.QuestionnairesService;
import org.fao.sws.model.config.questionnaire.QuestionnaireConfiguration;
import org.fao.sws.model.config.questionnaire.QuestionnairesConfiguration;
import org.fao.sws.model.dao.BlockMetadataDao;
import org.fao.sws.model.dao.impl.mapping.BlockMetadataQuestionnaireDto;
import org.fao.sws.model.dao.impl.mapping.DataProvider;
import org.fao.sws.model.filter.DimensionFilter;
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
	
	@Before 
	public void setup() {
		
		
		questionnaires.load();
		
		questionnaire = questionnaires.getQuestionnaire("ap-main");
		
		DimensionFilter M49inAgriculture = domain("agriculture")
									.with(dataset("agriculture")
									.with(dimension("geographicAreaM49").ref()))
									.filter();
		
		keyforFrance = dims.getDimensionValue(M49inAgriculture.setDimensionValueCode(codeforFrance)).getId();
		
		DimensionFilter yearsInAgriculture = domain("agriculture")
				.with(dataset("agriculture")
				.with(dimension("timePointYears").ref()))
				.filter();
		
		keyfor2013 = dims.getDimensionValue(yearsInAgriculture.setDimensionValueCode(codefor2013)).getId();
	}

	
	@Inject
	BlockMetadataDao dao;
	
	@Test @SneakyThrows
	public void query_can_be_executed() {
		
		@Cleanup DataProvider<BlockMetadataQuestionnaireDto> result = dao.getBlockMetadataProvider(questionnaire, keyforFrance, keyfor2013);

		assertFalse(result.isEmpty());

		BlockMetadataQuestionnaireDto dto = null;
		
		while ((dto=result.next())!=null)
			System.out.println(dto.value());

	}
	
	@Inject
	QuestionnairesService ejb;
	
	@Test
	public void questionnaire_with_block_metadata_can_be_generated() {
	
		File file = new File("target/q.xls");
		
		ejb.createQuestionnaire(questionnaire, 
								questionnaires.getMetadataConfigurations(), 
								codefor2013, 
								codeforFrance, 
								file);
		
	}

}
