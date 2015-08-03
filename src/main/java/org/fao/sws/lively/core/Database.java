package org.fao.sws.lively.core;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.fao.sws.lively.modules.Common.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;


/*
 * Stereotype for all test beans that should be injected in production code. 
 */


@Target({TYPE})
@Retention(RUNTIME)
public @interface Database {
	
	Source value() default Source.LOCAL;
	
	public enum Source {LOCAL,QA,PROD}
	
	public class RemoteRule implements TestRule {
		
		@Override
		public Statement apply(Statement base, Description description) {
			
			Database db = description.getAnnotation(Database.class);
			
			if (db!=null) {
				
				log.info("*** using {} database with local config ***",db.value());
				
				Producers.source = db.value();
			}
			
			return base;
			
		}
	}
}
