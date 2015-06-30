package org.fao.sws.lively.core;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.sql.DataSource;

import lombok.SneakyThrows;

import org.fao.sws.model.cdi.SwsScope;
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
	
	@Resource
	DataSource source;
	
	@Resource
	ConnectionFactory queues;
	
	
	@Produces @ApplicationScoped @TestDouble @SwsScope
	@SneakyThrows 
	DataSource localdb() {
		return source;
	}
	
	@Produces @ApplicationScoped @TestDouble @SwsScope
	@SneakyThrows 
	JmsTemplate jmsTemplate() {
		JmsTemplate template = new JmsTemplate(queues);
		template.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		return template;
	}
	
}
