package org.fao.sws.lively.modules;

import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import lombok.experimental.UtilityClass;

import org.fao.sws.domain.plain.reference.DataSet;
import org.fao.sws.ejb.DataSetService;
import org.fao.sws.lively.LiveTest;

@UtilityClass
public class Configuration extends Common {
		
		void startup(@Observes LiveTest.Start e, DataSetService services) {
			
			Configuration.datasetservice = services;
		}
		
		
	@Inject
	DataSetService datasetservice;
	
	//one
	
	public DataSet aDataset() {
		return oneof(datasets());
	}
	
	public DataSet dataset(String name) {
		return datasetservice.getDataSetByCode(name);
	}
	
	public DataSet aDatasetThat(Predicate<DataSet> filter) {
		return oneof(datasetsThat(filter));
	}
	
	//many
	public Stream<DataSet> datasets() {
		
		return configuration.getDomains().stream()
										.flatMap(d->d.getDataSets().stream())
										.map(d->dataset(d.getCode()));
		
	}
	
	public Stream<DataSet> datasetsThat(Predicate<DataSet> filter) {
		return datasets().filter(filter);
	}
	
	
}
