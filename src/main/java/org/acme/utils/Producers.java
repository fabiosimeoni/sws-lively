package org.acme.utils;

import javax.annotation.Priority;
import javax.ejb.TimerService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.jms.Queue;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import lombok.SneakyThrows;

import org.acme.utils.Transactions.TestSource;
import org.fao.sws.model.cdi.SwsScope;
import org.postgresql.ds.PGSimpleDataSource;

@ApplicationScoped @Priority(1) //paired with @Alternative, beats no-priority production producers 
public class Producers {

	@Produces @ApplicationScoped @SneakyThrows
	DataSource source() {
		
		PGSimpleDataSource source = new PGSimpleDataSource();
		source.setDatabaseName("sws_data");
		//take a full admin
		source.setUser("fabio");
		source.setPassword("fabio");
		
		return new TestSource(source);
	}
	
	@Produces @Alternative @SwsScope 
	DataSource source(DataSource source) {
		return source;
	}
	
	@Produces @ApplicationScoped
	Queue noqueue() {
		return null;
	}
	
	@Produces @ApplicationScoped
	TimerService notimer() {
		return null;
	}
	
	@Produces @ApplicationScoped
	UserTransaction notransaction() {
		return null;
	}
	
	

}
