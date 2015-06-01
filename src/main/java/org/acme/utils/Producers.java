package org.acme.utils;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.sql.DataSource;

import org.fao.sws.model.cdi.SwsScope;
import org.postgresql.ds.PGSimpleDataSource;

@Priority(1) //paired with @Alternative, beats no-priority production producers 
public class Producers {

	@Produces @ApplicationScoped
	DataSource source() {
		PGSimpleDataSource source = new PGSimpleDataSource();
		source.setDatabaseName("sws_data");
		source.setUser("sws");
		source.setPassword("sws");
		return source;
	}
	
	@Produces @Alternative @SwsScope 
	DataSource source(DataSource source) {
		return source;
	}
}
