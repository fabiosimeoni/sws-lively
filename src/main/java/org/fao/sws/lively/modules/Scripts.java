package org.fao.sws.lively.modules;

import static org.fao.sws.lively.modules.Common.*;

import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;

import lombok.experimental.UtilityClass;

import org.fao.sws.domain.plain.operational.ComputationModule;
import org.fao.sws.domain.plain.operational.ComputationScheduled;
import org.fao.sws.domain.plain.operational.ComputationScheduled.ComputationScheduledParameters;
import org.fao.sws.ejb.ComputationModuleService;
import org.fao.sws.ejb.ComputationScheduledService;
import org.fao.sws.lively.SwsTest.Start;
import org.fao.sws.model.filter.ComputationModuleFilter;

@UtilityClass
public class Scripts extends DomainModule {
	
	void startup(@Observes Start e, ComputationModuleService modules, ComputationScheduledService scheduled) {
		
		Scripts.scheduledservice = scheduled;
		Scripts.moduleservice=modules;
	}
	
	
	public ComputationScheduledService scheduledservice;
	
	public ComputationModuleService moduleservice;
	
	
	
	public String mainDatasetOf(ComputationModule module) {
		return module.getMetadata().getDatasets().getMainDatasetsList().get(0).getCode();
	}
	
	public Predicate<ComputationModule> overMain(String dataset) {
		return m -> mainDatasetOf(m).equals(dataset);
	}
	
	
	public Stream<ComputationModule> modules() {
		return shuffle(moduleservice.search(new ComputationModuleFilter()));
	}
	
	public ComputationModule aModule() {
		
		return aModuleThat($->true); //dry
	}
	
	public Stream<ComputationModule> historyOf(ComputationModule module) {
		
		return moduleservice.search(new ComputationModuleFilter()
							.setName(module.getName())
							.setIsWithHistory(true)
							.setCore(module.isCoreScript())).stream();
	}
	
	public ComputationModule aModuleThat(Predicate<ComputationModule> filter) {
		
		ComputationModule module = oneof(modules().filter(filter));
		log.info("module {} by {}",module.getName(), module.getOwner().getUsername());
		return module;
	}
	
	public ComputationModule aModuleOver(String dataset) {
		
		return oneof(modules().filter(overMain(dataset)));
	}
	
	public ComputationScheduled schedule(ComputationModule module) {
		
		return scheduledservice.create(module, new ComputationScheduledParameters(), module.getOwner().getId());
	}

}
