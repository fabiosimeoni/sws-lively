package org.fao.sws.lively.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainModule {

	/**
	 * shared logger.
	 * <p>
	 * (this is for domain modules, test output stands out better with {@link Common#show()} or {@link System#out}).
	 */
	public static Logger log = LoggerFactory.getLogger("test");


	/** shows it. */
	public static <T> void show(Object text) { //just for consistency and looks.
		
		System.out.println(text);
	}
	
	// general clauses to encourage sentence-based APIs
	// (sacrifices documentation to the altar of reuse)

	public interface ToClause<T, S> {

		S to(T t);
	}

	public interface OverClause<T, S> {

		S over(T t);
	}

	public interface FromClause<T, S> {

		S from(T t);
	}

	public interface WithClause<T, S> {

		S with(T t);
	}

	public interface AtClause<T, S> {

		S at(T t);
	}

	public interface InClause<T, S> {

		S in(T t);
	}

	public interface ExceptClause<T, S> {

		S except(T t);

	}

}
