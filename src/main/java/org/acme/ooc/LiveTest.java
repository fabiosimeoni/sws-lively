package org.acme.ooc;

import static java.lang.String.*;
import static java.lang.System.*;
import static org.acme.ooc.Common.*;
import static org.acme.ooc.Users.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.ejb.embeddable.EJBContainer;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.transaction.UserTransaction;

import lombok.SneakyThrows;

import org.acme.ooc.support.SecurityContext;
import org.apache.openejb.api.LocalClient;
import org.apache.openejb.util.Slf4jLogStreamFactory;
import org.fao.sws.domain.plain.operational.User;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.bridge.SLF4JBridgeHandler;


@LocalClient
public class LiveTest {
	


	public static class Start {}
	
	public static EJBContainer container;
	
	@Inject
	protected static UserTransaction tx;
	
	@Inject
	protected static Event<Start> events;
	
	@Resource
	ConnectionFactory factory;
	
	static SecurityContext sctx;
	
	protected User currentuser;
	
	
	//////////    test management  //////////////////////////////////////////////////////////////////////////////////
	
	@BeforeClass
	public static void startContainer() {
	
		configure_local_environment();

		configure_logging();

		start_container();

	}
	
	@Before @SneakyThrows
	public void startTest() {
		
		inject_testcase();
		
		events.fire(new Start());
		
		tx.begin();
		
		login(anAdmin());
	}
	
	
	@After @SneakyThrows
	public void endTest() {
		
		tx.rollback();
		
		logout(); //a bit academic...
	}
	
	static void configure_local_environment() {
	
		System.setProperty(DatabaseConfiguration.CONFIG_ROOT_PROPERTY, "src/main/resources");
	}
	

	static void configure_logging() {
		
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}
	
	
	@SneakyThrows
	static void start_container() {
		
		log.info("starting container...");
		
		Properties properties = new Properties();
		
		InputStream stream = LiveTest.class.getResourceAsStream("/connection.properties");
		
		if (stream==null)
			throw new RuntimeException("what happened to connection.properties??");
		
		properties.load(stream);
		
		Map<String,String> props = new HashMap<>();
		
		props.put("db", "new://Resource?type=DataSource");
		props.put("db.JdbcDriver", "org.postgresql.xa.PGXADataSource");
		props.put("db.JdbcUrl", format("jdbc:postgresql:%s",properties.getProperty("db")));
		props.put("db.UserName", properties.getProperty("user"));
		props.put("db.Password", properties.getProperty("password"));
		props.put("db.JtaManaged", "true");
		
		props.put("openejb.log.factory", Slf4jLogStreamFactory.class.getCanonicalName()); //slf4j
		props.put("org.slf4j.simpleLogger.log.org.apache", "warn");
		props.put("org.slf4j.simpleLogger.log.OpenEJB", "warn");						  //quiet	
		props.put("openejb.deployments.classpath.include", ".*/target/classes/");		  //lean
		
		
		long now = currentTimeMillis();
		
		container = EJBContainer.createEJBContainer(props);
			
		log.info("container ready in {} secs.", (float)(currentTimeMillis()-now)/1000);
		
	}
	
	@AfterClass
	public static void stopContainer() {
		
		//clashes with shutdown hook of jms broker
		//pointless anyway as this test is live and all dies when we finish.
		//container.close();
		
	}

	@SneakyThrows
	void inject_testcase() {
		container.getContext().bind("inject",this);
	}
	
	
}
