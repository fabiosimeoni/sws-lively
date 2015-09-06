package org.fao.sws.lively.core;

import static org.fao.sws.lively.core.Database.Source.*;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.resource.spi.IllegalStateException;
import javax.sql.DataSource;

import lombok.SneakyThrows;

import org.fao.sws.domain.plain.operational.EditingSession;
import org.fao.sws.domain.plain.operational.Tag;
import org.fao.sws.ejb.DimensionService;
import org.fao.sws.model.cdi.SwsScope;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.fao.sws.model.i18n.LocaleResolver;
import org.fao.sws.web.repository.VisualizationDataSource;
import org.fao.sws.web.repository.VisualizationRepository;
import org.fao.sws.web.repository.impl.VisualizationRepositoryImpl;
import org.springframework.jms.core.JmsTemplate;

/**
 * Produce test doubles with precedence over production beans.
 * Reason here is to bypass access to JNDI that doesn't work with openejb.
 * So this is not a feature, it's a (nice) workaround to bypass non portable JNDI access.
 */
@ApplicationScoped 
@TestDouble //gotta mark the class as double _as well as_ the produced beans
public class Producers {

	//injecting is the easiest way to ignore JNDI
	
	public static Database.Source source = LOCAL;
	
	@Resource(name="db-local")
	DataSource local_source;
	
	@Resource(name="db-qa")
	DataSource qa_source;
	
	@Resource(name="db-prod")
	DataSource prod_source;
	
	@Resource
	ConnectionFactory queues;
	
	
	@Produces @ApplicationScoped @TestDouble @SwsScope
	@SneakyThrows 
	DataSource source() {
		
		switch(source) {
		
			case LOCAL : return local_source;
			case QA : return qa_source ;
			case PROD : return prod_source;
		}
		
		throw new IllegalStateException("unknown source "+source);
	}
	
	@Produces @ApplicationScoped @TestDouble @SwsScope
	@SneakyThrows 
	JmsTemplate jmsTemplate() {
		JmsTemplate template = new JmsTemplate(queues);
		template.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		return template;
	}
	
	
	
	// cdi 1.0 in tests picks also the implementation class as a bean, which creates ambiguity.
	// we are forced to produce higher-priority beans to resolve it from here.
	
	@Produces @ApplicationScoped @TestDouble
	VisualizationRepository<EditingSession> getSessionVisualizationRepository(
			VisualizationDataSource<EditingSession> visualizationDataSource,
			DatabaseConfiguration config, 
			DimensionService dimensionService,
			LocaleResolver localeResolver) {
		
		return new VisualizationRepositoryImpl<>(visualizationDataSource,config, dimensionService, localeResolver);
	}
	
	@Produces @SessionScoped @TestDouble
	VisualizationRepository<Tag> getTagVisualizationRepository(
			VisualizationDataSource<Tag> visualizationDataSource,
			DatabaseConfiguration config, DimensionService dimensionService,
			LocaleResolver localeResolver) {
		
		return new VisualizationRepositoryImpl<>(visualizationDataSource,config, dimensionService, localeResolver);
	}
	
}
