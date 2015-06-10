package org.acme.utils;

import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import org.apache.deltaspike.core.util.metadata.AnnotationInstanceProvider;
import org.apache.deltaspike.core.util.metadata.builder.AnnotatedTypeBuilder;

public class CdiExtension implements Extension {

	static final RequestScoped request_scoped = AnnotationInstanceProvider.of(RequestScoped.class);
	static final Inject inject = AnnotationInstanceProvider.of(Inject.class);
	static final Singleton singleton = AnnotationInstanceProvider.of(Singleton.class);
	static final ApplicationScoped application_scoped = AnnotationInstanceProvider.of(ApplicationScoped.class);

	public <X> void processInjectionTarget(@Observes @WithAnnotations({ Stateless.class, MessageDriven.class, Interceptor.class, Singleton.class }) ProcessAnnotatedType<X> pat) {
		
		AnnotatedType<X> at = pat.getAnnotatedType();
		AnnotatedType<X> replaced = null;
		
		if (at.isAnnotationPresent(Stateless.class) || at.isAnnotationPresent(MessageDriven.class)) 
			replaced = replaceIn(at).addToClass(request_scoped).create();
		
		else if (at.isAnnotationPresent(Interceptor.class))
			replaced = replaceIn(at).create();
		
		else if (at.isAnnotationPresent(Singleton.class))
			replaced = replaceIn(at).addToClass(application_scoped).create();
		
		if (replaced!=null)
			pat.setAnnotatedType(replaced);
	
	}

	// Adds @Inject to all the dependencies of the interceptor.
	private <X> AnnotatedTypeBuilder<X> replaceIn(AnnotatedType<X> original) {
		
		AnnotatedTypeBuilder<X> replacement = new AnnotatedTypeBuilder<X>().readFromType(original);
		
		original.getFields().stream().filter(withResourceOrEjbAnnotation).forEach(f->replacement.addToField(f, inject));
		//original.getMethods().stream().filter(withResourceOrEjbAnnotation).forEach(f->replacement.addToMethod(f, inject));
    	
    	
		return replacement;
	}
	
	
	private static Predicate<AnnotatedMember<?>> withResourceOrEjbAnnotation = m->!m.isAnnotationPresent(Inject.class) 
															&& 
															Stream.of(Resource.class,EJB.class)
																	.filter(m::isAnnotationPresent)
																	.findFirst()
																	.isPresent();


}
