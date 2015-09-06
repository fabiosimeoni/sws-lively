package org.fao.sws.lively;

import static java.lang.System.*;
import static org.fao.sws.lively.modules.Users.*;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ejb.embeddable.EJBContainer;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.transaction.UserTransaction;

import lombok.SneakyThrows;

import org.apache.openejb.api.LocalClient;
import org.apache.openejb.util.Slf4jLogStreamFactory;
import org.fao.sws.domain.plain.util.ConfigLocations;
import org.fao.sws.lively.core.Database.RemoteRule;
import org.fao.sws.lively.modules.Common;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.slf4j.bridge.SLF4JBridgeHandler;


/** Extend it from your test suites to enable integration testing. */
// infrastructure must be encapsulated here: shouldn't leak into tests.
// may evolve into proper Junit Runner.

@LocalClient
public class SwsTest extends Common { //OpenEJB requires it not to be abstract, or else no injection.

	//these scream for a common base class, but CDI 1.0 doesn't like it. Reconsider with Java EE7.
	public static class Start {} 
	public static class End {}

	//takes care of @Database selection (will be obsolete if this becomes a runner)
	@ClassRule public static RemoteRule rule = new RemoteRule();

	
	// suite-wide state
	
	@Inject	static UserTransaction tx;
	
	@Inject static Event<Start> start;
	
	@Inject	static Event<End> end;
	
	static EJBContainer container;
	
	// test-wide state
	
	
	
	//////////   test management  //////////////////////////////////////////////////////////////////////////////////
	
	@BeforeClass
	public static void startContainer() {
	
		configure_local_environment();

		configure_logging();

		start_container();

	}
	
	@Before @SneakyThrows
	public void startTest() {
		
		inject_testcase();
		
		start.fire(new Start());
		
		tx.begin();
		
		login(anAdmin());
	}
	
	
	@After @SneakyThrows
	public void endTest() {
		
		tx.rollback();
		
		logout(); //a bit academic...
		
		end.fire(new End());
	}
	
	
	
	static void configure_local_environment() {
	
		ConfigLocations.root=new File("src/main/resources/config");
	}
	
	static void configure_logging() {
		
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}
	
	
	@SneakyThrows
	static void start_container() {
		
		log.info("starting container...");
		
		Properties properties = new Properties();
		
		InputStream stream = SwsTest.class.getResourceAsStream("/connection.properties");
		
		if (stream==null)
			throw new RuntimeException("what happened to connection.properties??");
		
		properties.load(stream);
		
		Map<String,String> props = new HashMap<>();
		
		//local
		props.put("db-local", "new://Resource?type=DataSource");
		props.put("db-local.JdbcDriver", "org.postgresql.xa.PGXADataSource");
		props.put("db-local.JdbcUrl", properties.getProperty("url-local"));
		props.put("db-local.UserName", properties.getProperty("user-local"));
		props.put("db-local.Password", properties.getProperty("password-local"));
		props.put("db-local.JtaManaged", "true");
		
		//QA
		props.put("db-qa", "new://Resource?type=DataSource");
		props.put("db-qa.JdbcDriver", "org.postgresql.xa.PGXADataSource");
		props.put("db-qa.JdbcUrl", properties.getProperty("url-qa"));
		props.put("db-qa.UserName", properties.getProperty("user-qa"));
		props.put("db-qa.Password", properties.getProperty("password-qa"));
		props.put("db-qa.JtaManaged", "true");
		
		//prod
		props.put("db-prod", "new://Resource?type=DataSource");
		props.put("db-prod.JdbcDriver", "org.postgresql.xa.PGXADataSource");
		props.put("db-prod.JdbcUrl", properties.getProperty("url-prod"));
		props.put("db-prod.UserName", properties.getProperty("user-prod"));
		props.put("db-prod.Password", properties.getProperty("password-prod"));
		props.put("db-prod.JtaManaged", "true");
		
		
		props.put("openejb.log.factory", Slf4jLogStreamFactory.class.getCanonicalName()); //slf4j
		props.put("org.slf4j.simpleLogger.log.org.apache", "warn");
		props.put("org.slf4j.simpleLogger.log.OpenEJB", "warn");  //quiet						
		props.put("openejb.deployments.classpath.include", ".*(sws-.*|/target/classes/)");
		//props.put("openejb.deployments.classpath.include", ".*/target/classes/");		  //lean
		
		
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
