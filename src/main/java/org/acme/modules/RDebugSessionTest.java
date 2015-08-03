package org.acme.modules;

import static java.util.Collections.*;
import static org.fao.sws.lively.modules.Common.*;
import static org.fao.sws.lively.modules.Configuration.*;
import static org.fao.sws.lively.modules.Modules.*;
import static org.fao.sws.lively.modules.Sessions.*;
import static org.fao.sws.lively.modules.Users.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.fao.sws.domain.plain.operational.ComputationExecution;
import org.fao.sws.domain.plain.operational.ComputationModule;
import org.fao.sws.domain.plain.operational.EditingSession;
import org.fao.sws.domain.plain.operational.User;
import org.fao.sws.domain.plain.reference.DataSet;
import org.fao.sws.ejb.data.DataRequestDto;
import org.fao.sws.lively.SwsTest;
import org.fao.sws.model.dao.ComputationExecutionDao;
import org.fao.sws.web.dto.ComputationExecutionRequestDto;
import org.fao.sws.web.dto.ComputationExecutionWithParametersDto;
import org.fao.sws.web.dto.InteractiveValidationRequestDto.ComputationExecutionDataSetDto;
import org.fao.sws.web.rest.ComputationExecutionRest;
import org.fao.sws.web.rest.r.RComputationParameters;
import org.fao.sws.web.rest.r.RDataRest;
import org.junit.Test;

public class RDebugSessionTest extends SwsTest {

	@Inject
	ComputationExecutionRest computationService;

	@Inject
	RComputationParameters sessionlookupService;
	
	@Inject
	RDataRest computationDataService;
	
	@Inject
	ComputationExecutionDao dao;

//	@Test(expected=UnauthorizedApplicationException.class)
//	public void sessions_cannot_be_started_by_any_user() {
//		
//		ComputationModule module = aModule();
//		
//		login(aNewUser());
//		
//		ComputationExecutionRequestDto request = prepareRequestOver(module);
//		
//		computationService.run(request);
//		
//	}
	
	@Test
	public void sessions_can_be_started_by_administrators() {
		
		ComputationModule module = aModule();
		
		login(aNewUserIn(anAdminGroup()));
		
		ComputationExecutionRequestDto request = prepareRequestOver(module);
		
		Response response = computationService.run(request);
		
		UUID token = singleobjectIn(response);
		
		assertNotNull(token);
	}
	
	
	@Test
	public void sessions_can_be_started_by_module_owners() {
		
		ComputationModule module = aModule();
		
		login(module.getOwner());
		
		ComputationExecutionRequestDto request = prepareRequestOver(module);
		
		Response response = computationService.run(request);
		
		UUID token = singleobjectIn(response);
		
		assertNotNull(token);
	}
	
	
	@Test
	public void sessions_create_executions() {
		
		ComputationModule module = aModule();
		
		User user = aNewUser();
		
		login(user);
		
		ComputationExecutionRequestDto request = prepareRequestOver(module);
		
		Response response = computationService.run(request);
		
		UUID token = singleobjectIn(response);
		
		ComputationExecution exec = dao.findByToken(token);
		
		assertNotNull(exec);
		
		assertEquals("execution is of given module", module, exec.getComputationModule());
		assertEquals("execution has expected token", token, exec.getToken());
		assertEquals("execution belongs to logged user", user, exec.getUser());
		assertEquals("execution can be retrieved via its id", exec, dao.findByIdAndToken(exec.getId(), token));
	
	}
	
	@Test
	public void sessions_can_be_looked_up_by_token() {
		
		ComputationExecutionRequestDto request = prepareRequestOver(aModule());
		
		Response response = computationService.run(request);
		
		UUID token = singleobjectIn(response);
		
		response = sessionlookupService.getComputationParameters(token);
		
		ComputationExecutionWithParametersDto execution = singleobjectIn(response);
		
		assertNotNull("response carries execution id",execution.getId());
		
	}
	
	@Test
	public void getdata_can_step_outside_main_dataset() {
		
		ComputationExecutionWithParametersDto execution = stageExecution();
		
		ComputationExecutionDataSetDto main = execution.getDatasets().get(0);
		
		DataSet targetdataset = dataset(main.getDataSetCode());
		DataSet otherdataset = aDatasetThat(isOtherThan(targetdataset));
		
		DataRequestDto datarequest = new DataRequestDto();
		datarequest.setToken(execution.getToken());
		datarequest.setDomain(otherdataset.getDomain().getCode());
		datarequest.setDataSet(otherdataset.getName());
		
		Response response = computationDataService.getData(execution.getId(),datarequest);
		
		consume(response);
		
		
	}
	
	
	@Test
	public void getdata_in_session() {
		
		ComputationExecutionWithParametersDto execution = stageExecution();
		
		ComputationExecutionDataSetDto main = execution.getDatasets().get(0);
		
		DataRequestDto datarequest = new DataRequestDto();
		datarequest.setToken(execution.getToken());
		datarequest.setDomain(main.getDataSetCode());
		datarequest.setDataSet(main.getDomainCode());
		
		Response response = computationDataService.getData(execution.getId(),datarequest);
		
		consume(response);
		
		
	}
	
	
	/////   helpers ////////////////////////////////////////////////////////////////////////


	private ComputationExecutionWithParametersDto stageExecution() {
		
		ComputationExecutionRequestDto request = prepareRequestOver(aModule());
		
		Response response = computationService.run(request);
		
		UUID token = singleobjectIn(response);
		
		response = sessionlookupService.getComputationParameters(token);
		
		ComputationExecutionWithParametersDto execution = singleobjectIn(response);
		
		return execution;
	}
	
	private ComputationExecutionRequestDto prepareRequestOver(ComputationModule module) {
		
			/////////////////////////////////////////////////////////////////// main dataset info
					
			//runs over this dataset (could infer from module def!)
			DataSet dataset = dataset(mainDatasetOf(module));
			
			
			//to run over this subset of the dataseta
			Map<String,List<String>> cube = new HashMap<>();
			
			EditingSession session = aNewSession().over(dataset);
			
			session.getDimension2ids().forEach((k,v)-> cube.put(k, all(all(v).map(id->id.toString()))));
			
			//use only session's cube for core module, otherwise use actual session 
			Long sessionId = module.isCoreScript() ? null : session.getId(); 
			
			ComputationExecutionDataSetDto main = new ComputationExecutionDataSetDto(null, dataset.getName(), sessionId);
			main.setDimension2codes(cube);
			
			//////////////////////////////////////////////////////////////////////////////////////////////////////  request
			
			ComputationExecutionRequestDto request = new ComputationExecutionRequestDto();
			request.setDebug(yep);
			request.setComputationModuleId(module.getId());
			request.setAsync(nope);
			request.setMainDataSet(main);
			request.setDataSets(emptyMap());  	//no auxiliary datasets
			request.setParameters(emptyMap());  //no parameters for now
			
			return request;
			
	}
	
}


