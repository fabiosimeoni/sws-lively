package org.fao.sws.lively.modules;

import static org.fao.sws.lively.modules.Common.*;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import org.fao.sws.domain.plain.reference.DataSet;
import org.fao.sws.domain.plain.reference.DimensionValue;
import org.fao.sws.ejb.DataSetService;
import org.fao.sws.ejb.DimensionService;
import org.fao.sws.ejb.FlagService;
import org.fao.sws.lively.SwsTest.Start;
import org.fao.sws.model.config.DataSetConfiguration;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.fao.sws.model.config.Describable;
import org.fao.sws.model.config.DimensionConfiguration;
import org.fao.sws.model.config.FlagConfiguration;
import org.fao.sws.model.filter.DimensionFilter;
import org.fao.sws.model.filter.FlagFilter;

/** Utilities for tests that work with configuration */
@UtilityClass
public class Configuration extends DomainModule {
		
				//tricksies: simulates static injection via eventing.
				static void startup(@Observes Start e, 
													DatabaseConfiguration config,
													DataSetService datasets, 
													DimensionService dimensions,
													FlagService flags) {
				
					configuration = config;
					datasetservice = datasets;
					dimensionservice=dimensions;
					flagservice=flags;
				
				}
		
		
	
	DatabaseConfiguration configuration;	
	DataSetService datasetservice;
	DimensionService dimensionservice;
	FlagService flagservice;
	
	
	///////////////// general predicates /////////////////////////////////////////////////////////////////////////////////////////////
	
	/** only those with a given name. **/
	public Predicate<Describable> areCalled(String name) {
		return d -> d.getCode().equals(name);
	}
	
	
	
	
	
	///////////////// datasets and dataset configurations ///////////////////////////////////////////////////////////////////////////
	
	/** only if it has flags. **/
	public Predicate<DataSet> hasFlags = d -> ! configOf(d).observation().getFlagConfigurations().isEmpty();

	
	//many

	/** all known datasets. **/
	public Stream<DataSet> datasets() {
		
		//could use configuration, but this works also on remote sources, which is handy.
		Stream<String> names = all(datasetservice.getCompleteMapOfDatasetsPerDomain().values()).flatMap(ds->all(ds.keySet()));
		
		return Common.shuffle(names).map(code->datasetservice.getDataSetByCode(code));
		
	}
	
	/** all dataset configurations. **/
	public Stream<DataSetConfiguration> datasetConfigs() {
		
		return shuffle(all(configuration.getDomains()).flatMap(d->all(d.getDataSets())));
		
	}
	
	
	/** only those of interest.  **/
	public Stream<DataSet> datasetsThat(@NonNull Predicate<DataSet> predicate) {
		return datasets().filter(predicate);
	}
	
	/** only those of interest.  **/
	public Stream<DataSetConfiguration> datasetsConfigsThat(@NonNull Predicate<? super DataSetConfiguration> filter) {
		return datasetConfigs().filter(filter);
	}

	//one
	
	public DataSet aDatasetThat(@NonNull Predicate<DataSet> filter) {
		DataSet ds  = oneof(datasetsThat(filter));
		log.info("dataset {}",ds.getName());
		return ds;
	}
	
	public DataSetConfiguration aDatasetConfigThat(@NonNull Predicate<? super DataSetConfiguration> filter) {
		DataSetConfiguration ds  = oneof(datasetsConfigsThat(filter));
		log.info("dataset {}",ds.getCode());
		return ds;
	}

	/** any dataset, at random. **/
	public DataSet aDataset() {
		return aDatasetThat($->true);
	}
	
	/** any dataset configuration, at random. **/
	public DataSetConfiguration aDatasetConfig() {
		return aDatasetConfigThat($->true);
	}
	
	/** precisely this dataset configuration. **/
	public DataSetConfiguration datasetConfig(@NonNull String name) {
		return aDatasetConfigThat($->name.equals($.getCode()));
	}
	
	/** precisely this dataset. **/
	public DataSet dataset(@NonNull String name) {
		return aDatasetThat($->name.equals($.getName()));
	}
	

	/** the configuration of a given dataset. **/
	public DataSetConfiguration configOf(@NonNull DataSet dataset) {
		return configuration.getDataSetConfiguration(dataset.getDomain().getCode(),dataset.getName());
	}
	
	
	
	
	///////////////// dimensions //////////////////////////////////////////////////////////////////////////////////////////

	/** excludes hierarchical aggregations. **/
	public Predicate<DimensionValue> leaves = d -> ! d.isSubtreeSelectionOnly();
	
	/** pulls out those identifiers. **/
	public Function<DimensionValue,Long> coordinateids = d -> d.getId();
	
	/** pulls out those codes. **/
	public Function<DimensionValue,String> dimcodes = d -> d.getCode();
	
	
	//many
	public Stream<DimensionConfiguration> dimensions() {
		
		return shuffle(distinct(datasetConfigs().flatMap(c->all(c.getDimensions()))));
		
	}
	
	public Stream<DimensionConfiguration> dimensionsThat(Predicate<? super DimensionConfiguration> predicate) {
		
		return dimensions().filter(predicate);
		
	}
	public Stream<DimensionConfiguration> dimensionsOf(@NonNull DataSet dataset) {
		
		return all(configOf(dataset).getDimensions());
	}
	
	@SuppressWarnings("all")
	public Stream<DimensionValue> valuesOf(@NonNull DimensionConfiguration dim) {
		
		DataSetConfiguration ds = oneof(datasetsConfigsThat(
				
				c->all(c.getDimensions()).filter(isSameAs(dim)).findAny().isPresent()
			
		)); 
		
		DimensionFilter dims = new DimensionFilter()
											.setDataSetCode(ds.getCode())
											.setDomainCode(ds.domain().getCode())
											.setDimensionCode(dim.getCode());

		return (Stream) dimensionservice.getAllDimensions(dims).stream();
	}
	

	//one
	
	public DimensionConfiguration aDimension() {
		return aDimensionThat($->true);
	}
	
	public DimensionConfiguration aDimensionThat(@NonNull Predicate<? super DimensionConfiguration> filter) {
		DimensionConfiguration dim = oneof(dimensionsThat(filter));
		log.info("dimension {}",dim.getCode());
		return dim;
	}

	public DimensionConfiguration dimension(@NonNull String name) {
		
		return aDimensionThat(areCalled(name));
	}
	
	
	///////////////// flags /////////////////////////////////////////////////////////////////////////////////////////////

	
	//many

	public Stream<FlagConfiguration> flags() {
		
		return distinct(datasetConfigs().flatMap(c->all(c.observation().getFlagConfigurations())));
		
	}
	
	public Stream<FlagConfiguration> flagsThat(@NonNull Predicate<? super FlagConfiguration> predicate) {
		
		return flags().filter(predicate);
		
	}
	
	public Stream<FlagConfiguration> flagsOf(@NonNull DataSet dataset) {
		
		return shuffle(all(configOf(dataset).observation().getFlagConfigurations()));
	
	}
	

	
	public Stream<String> valuesOf(@NonNull FlagConfiguration flag) {
		
		//configuration is so messed up that we can get to flag tables only via
		// a dataset that references it...
		
		DataSetConfiguration ds = oneof(datasetConfigs().filter(
				
			 c->all(c.observation().getFlagConfigurations()).filter(isSameAs(flag)).findAny().isPresent()
			
		)); 
		
		FlagFilter flags = new FlagFilter()
											.setDataSetCode(ds.getCode())
											.setDomainCode(ds.domain().getCode())
											.setFlagCode(flag.getCode());

		return flagservice.getAllFlags(flags).stream().map(fv->fv.getValue());
	}
	
	//one
	
	public FlagConfiguration aFlag() {
		
		return aFlagThat($->true);
	}
	
	public FlagConfiguration aFlagThat(@NonNull Predicate<? super FlagConfiguration> filter) {
		FlagConfiguration flag = oneof(flagsThat(filter));
		log.info("flag {}",flag.getCode());
		return flag;
	}
	
	public FlagConfiguration flag(@NonNull String name) {
		
		return oneof(flagsThat(areCalled(name)));
	}
	

}
