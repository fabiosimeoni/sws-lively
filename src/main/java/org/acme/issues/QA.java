package org.acme.issues;

import static org.fao.sws.lively.core.Database.Source.*;
import static org.fao.sws.lively.modules.Common.*;
import static org.fao.sws.lively.modules.Users.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.core.StreamingOutput;

import lombok.SneakyThrows;

import org.codehaus.jackson.map.ObjectMapper;
import org.fao.sws.ejb.data.DataRequestDto;
import org.fao.sws.lively.SwsTest;
import org.fao.sws.lively.core.Database;
import org.fao.sws.web.dto.ComputationExecutionWithParametersDto;
import org.fao.sws.web.dto.InteractiveValidationRequestDto.ComputationExecutionDataSetDto;
import org.fao.sws.web.dto.ResponseWrapperSingleObj;
import org.fao.sws.web.rest.r.RComputationParameters;
import org.fao.sws.web.rest.r.RDataRest;
import org.junit.Test;

/**
 * These tests run against the data in QA with local configuration.
 *
 */
@Database(QA)
public class QA extends SwsTest {
	
	
	@Test
	public void smoketest() {
		
		show (users(), u-> u.getUsername());
		
	}
	

	ObjectMapper mapper = new ObjectMapper();
	
	@Inject
	RDataRest datarest;
	
	@Inject
	RComputationParameters paramsrest;
	
	@Test @SneakyThrows
	public void sws1027() {
		
		InputStream input = this.getClass().getResourceAsStream("sws-1027.txt");

		DataRequestDto dto = mapper.readValue(input, DataRequestDto.class);
		
		login(user("FAODOMAIN/kao"));
		
		StreamingOutput out = (StreamingOutput) datarest.getData(439L, dto).getEntity();
		
		out.write(new OutputStream() {
			
			int i=0;
			@Override
			public void write(int b) throws IOException {
				
				if (i>=100) {
					this.flush();
					i=0;
				}
				else
					i++;
			}
		});
	}
	
	@Test @SneakyThrows
	public void sws1028() {
		
		InputStream input = this.getClass().getResourceAsStream("sws-1028.txt");

		DataRequestDto dto = mapper.readValue(input, DataRequestDto.class);
		
		System.out.println(dto.getToken());
		
		login(user("FAODOMAIN/sorbara"));
		
		
		
		@SuppressWarnings("all")
		ResponseWrapperSingleObj<ComputationExecutionWithParametersDto> obj = (ResponseWrapperSingleObj) 
				paramsrest.getComputationParameters(UUID.fromString("38ec8184-82d9-4a8b-85ed-5b4a0a62555e")).getEntity();
		
		ComputationExecutionWithParametersDto paramdto = obj.getResult();
		

		Long execId = paramdto.getId();
		
		System.out.println(execId);

		for (ComputationExecutionDataSetDto ds : paramdto.getDatasets()) {
			System.out.println(ds.getDimensions2Codes());
			System.out.println(ds.getDataSetCode());
			System.out.println(ds.getSessionId());	
		}
		System.out.println(paramdto.getParameters());
		
		
		dto.setToken("38ec8184-82d9-4a8b-85ed-5b4a0a62555e");
		StreamingOutput out = (StreamingOutput) datarest.getData(2200L, dto).getEntity();
		
		out.write(new OutputStream() {
			
			int i=0;
			@Override
			public void write(int b) throws IOException {
				
				if (i>=100) {
					this.flush();
					i=0;
				}
				else
					i++;
			}
		});
	}
}
