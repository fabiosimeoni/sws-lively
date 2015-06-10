package org.acme.utils;

import java.io.Closeable;
import java.sql.Connection;
import java.util.logging.Logger;

import javax.sql.DataSource;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Transactions {

	static Logger log = Logger.getLogger("test");
	
	// a single connection source; 
	public class TestSource implements DataSource {
	
	
		@Delegate(excludes={DataSourceExcludes.class, Closeable.class })
		final DataSource source;
	
		TestConnection conn;
		
		@SneakyThrows
		TestSource(DataSource source) {
			
			this.source=source;
			
			this.conn = new TestConnection(source.getConnection());
		}
		
		public Connection getConnection() {
			return conn;		
		}
		
		
	
	}	
	
	//a connection for a long-running transaction
	static class TestConnection implements Connection {
		
		@Delegate(excludes=ConnDelegateExcludes.class)
		private Connection conn;
		
		@SneakyThrows
		public TestConnection(Connection c) {
			(conn=c).setAutoCommit(false);	
		}
		
		
		public void close() {} //suppress attempts to return-to-pool
		
		@SneakyThrows
		public void rollback() {	
			
			log.info("rolling back test transaction");
			
			conn.rollback();
			
			@Cleanup Connection toclose = conn; //re-assigns to cleanup via lombok
			
		}
		
	}
	
	
	interface ConnDelegateExcludes {
		
		void close();
		
		void rollback();
		
	}
	
	interface DataSourceExcludes {
		
		Connection getConnection();
	}

}
