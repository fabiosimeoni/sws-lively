package org.acme.utils;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.sql.DataSource;

import lombok.SneakyThrows;

import org.acme.utils.Transactions.TestSource;
import org.postgresql.ds.PGSimpleDataSource;

@ApplicationScoped @Priority(1) //paired with @Alternative, beats no-priority production producers 
public class LocalSource {

	@Produces @ApplicationScoped @SneakyThrows
	DataSource source() {
		
		PGSimpleDataSource source = new PGSimpleDataSource();
		source.setDatabaseName("sws_data");
		//take a full admin
		source.setUser("fabio");
		source.setPassword("fabio");
		
		return new TestSource(source);
	}
}
