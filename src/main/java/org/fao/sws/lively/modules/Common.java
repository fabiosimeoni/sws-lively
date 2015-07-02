package org.fao.sws.lively.modules;

import static com.google.common.base.Objects.*;
import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;

import org.fao.sws.domain.Persistable;
import org.fao.sws.lively.LiveTest;
import org.fao.sws.model.config.DatabaseConfiguration;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Common {

	public static Logger log = LoggerFactory.getLogger("test");
	
	
	public static DatabaseConfiguration configuration;
	
	static void startup(@Observes LiveTest.Start e, DatabaseConfiguration config) {
		
		Common.configuration = config;
	}

	
	
	public static <T> void show(Stream<T> stream, Function<T,?> mapfunc) {
		
		stream.map(mapfunc).forEach(e->System.out.println(e));
	}
	
	public static <T> void show(Stream<T> stream) {
		
		show(stream, Function.identity());
	}
	
	public static <T> T oneof(Stream<T> stream) {
		return stream.findFirst().orElseThrow(()->new IllegalStateException("no such element, stage your db to match."));
	}
	
	public static <T> List<T> all(Stream<T> stream) {
		return stream.collect(toList());
	}
	
	public static <T> Stream<T> all(Collection<T> coll) {
		return coll.stream();
	}
	
	public static <T extends Persistable> Predicate<T> otherThan(T thisone) {
		return t->!equal(t.getId(), thisone.getId());
	}
	
	public static <T extends Persistable> Stream<Long> idsOf(Stream<T> identifiable) {
		return identifiable.map(t->t.getId());
	}
	
	public static void assertEquals(Persistable t1, Persistable t2) {
		Assert.assertEquals(t1.getId(), t2.getId());
	}
	
	

	public static interface ToClause<T,S> {
		
		S to(T t);
	}
	
	public static interface OverClause<T,S> {
		
		S over(T t);
	}
	
	
	public interface FromClause<T,S> {
	
		S from(T t);
	}
	
}
