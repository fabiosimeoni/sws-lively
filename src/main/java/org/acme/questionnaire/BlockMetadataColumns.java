package org.acme.questionnaire;

import static org.fao.sws.model.configuration.Dsl.*;
import static org.junit.Assert.*;

import javax.inject.Inject;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;

import org.acme.utils.Extensions;
import org.acme.utils.OocTest;
import org.fao.sws.ejb.DimensionService;
import org.fao.sws.model.config.questionnaire.QuestionnaireConfiguration;
import org.fao.sws.model.config.questionnaire.QuestionnairesConfiguration;
import org.fao.sws.model.dao.BlockMetadataDao;
import org.fao.sws.model.dao.impl.mapping.BlockMetadataQuestionnaireDto;
import org.fao.sws.model.dao.impl.mapping.DataProvider;
import org.fao.sws.model.filter.DimensionFilter;
import org.junit.Before;
import org.junit.Test;

@ExtensionMethod(Extensions.class)
public class BlockMetadataColumns extends OocTest {

	QuestionnairesConfiguration questionnaires = new QuestionnairesConfiguration();
	QuestionnaireConfiguration questionnaire;
	
	long keyforFrance;
	long keyfor2013;
	
	@Inject
	DimensionService dims;
	
	@Before public void setup() {
		
		
		questionnaires.load();
		
		questionnaire = questionnaires.getQuestionnaire("ap-main");
		
		DimensionFilter M49inAgriculture = domain("agriculture")
									.with(dataset("agriculture")
									.with(dimension("geographicAreaM49").ref()))
									.filter();
		
		keyforFrance = dims.getDimensionValue(M49inAgriculture.setDimensionValueCode("250")).getId();
		
		DimensionFilter yearsInAgriculture = domain("agriculture")
				.with(dataset("agriculture")
				.with(dimension("timePointYears").ref()))
				.filter();
		
		keyfor2013 = dims.getDimensionValue(yearsInAgriculture.setDimensionValueCode("2013")).getId();
	}

	
	@Inject
	BlockMetadataDao dao;
	
	@Test @SneakyThrows
	public void query_can_be_executed() {
		
		@Cleanup DataProvider<BlockMetadataQuestionnaireDto> result = dao.getBlockMetadataProvider(questionnaire, keyforFrance, keyfor2013);

		assertFalse(result.isEmpty());

		BlockMetadataQuestionnaireDto dto = null;
		
		while ((dto=result.next())!=null)
			log.info(dto.value());

	}

}
