package org.acme.utils;

import static java.lang.String.*;
import static java.lang.System.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.ejb.embeddable.EJBContainer;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.transaction.UserTransaction;

import lombok.SneakyThrows;

import org.apache.openejb.api.LocalClient;
import org.apache.openejb.util.Slf4jLogStreamFactory;
import org.fao.sws.domain.plain.operational.Group;
import org.fao.sws.domain.plain.operational.User;
import org.fao.sws.ejb.GroupService;
import org.fao.sws.ejb.UserService;
import org.fao.sws.ejb.security.SecurityContextCreator;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;


@LocalClient
public class LiveTest {
	
	public static Logger log = LoggerFactory.getLogger("test");
	
	public static EJBContainer container;
	
	@Inject
	protected static UserService allusers;
	
	@Inject
	protected static UserTransaction tx;
	
	@Resource
	ConnectionFactory factory;
	
	static SecurityContextCreator sctx;
	
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
		
		tx.begin();
		
		login(aUserIn(anAdminGroup()));
	}
	
	
	@After @SneakyThrows
	public void endTest() {
		
		tx.rollback();
		
		//a bit academic...
		sctx.remove();
		
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
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	@Inject
	GroupService groups;
	
	public Group anAdminGroup() {
		
		
		return aGroup(true);
		
	}
	
	public Group aRegularGroup() {
		
		
		return aGroup(true);
		
	}
	
	public Group aGroup() {
		
		return groups.getAllGroups(true).get(0);
		
	}
	
	private Group aGroup(boolean admin) {
		
		
		return groups.getAllGroups(true).stream().filter(g->g.isAdministrator()==admin).findAny().get();
		
	}
	
	public User anAdmin() {
		
		return aUserIn(anAdminGroup());

	}
	
	public void login(User user) {
		
		log.info("logging in {}",user.getUsername());
		currentuser = user;
		
		if (currentuser==null)
			throw new IllegalArgumentException("test error: unknown user "+user);
		
		sctx = new SecurityContextCreator(currentuser,false);
		sctx.bind();
	}
	
	
	public User aUserIn(Group group) {
		
		return allusers.getUsersByGrpId(group.getId())
											  .stream()
											  .findAny()
											  .orElseThrow(()->new IllegalStateException("no users in group "+group.getName()));

		
	}
	
	
}
