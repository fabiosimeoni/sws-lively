package org.acme;

import static org.fao.sws.lively.modules.Configuration.*;

import java.util.stream.Stream;

import org.fao.sws.domain.plain.reference.DataSet;
import org.fao.sws.domain.plain.reference.DimensionValue;
import org.fao.sws.lively.SwsTest;
import org.fao.sws.model.config.DataSetConfiguration;
import org.fao.sws.model.config.DimensionConfiguration;
import org.fao.sws.model.config.FlagConfiguration;
import org.junit.Test;

public class ConfigurationTest extends SwsTest {
	
	
	@Test
	public void dataset_support() {
		
		//all datasets (configs)
		
		log.info("{} datasets", datasetConfigs().count());

		show(codesOf(datasetConfigs())); //or..show(datasetConfigs(),names);

		log.info("{} datasets", datasets().count());  //domain object
		
		
		//some dataset
		DataSetConfiguration config = aDatasetConfig();
		DataSet dataset = aDataset();
		
		//a given dataset
		config = datasetConfig(config.getCode());
		dataset = dataset(dataset.getName());
	}
	
	@Test
	public void dimension_support() {
	
		//all dimensions
		
		log.info("{} dimensions.", dimensions().count());
		
		show(codesOf(dimensions()));
		
		//some dimension
		DimensionConfiguration dimension = aDimension();
		
		//a given dimension
		DimensionConfiguration anotherDimension = dimension(dimension.getCode()); 
		
		assertEquals(anotherDimension,dimension);
		
		//values
		Stream<DimensionValue> values = valuesOf(dimension);
		
		//codes of leaf values
		show(values.filter(leaves),dimcodes);
		

	}
	
	@Test
	public void flag_support() {
	
		//all dimensions
		
		log.info("{} flags.", flags().count());
		
		//some dimension
		FlagConfiguration flag = aFlag();
		
		//a given dimension
		FlagConfiguration anotherFlag = flag(flag.getCode()); 
		
		assertEquals(anotherFlag,flag);
		
		//values
		Stream<String> values = valuesOf(flag);
		
		show(values);
		

	}
}
