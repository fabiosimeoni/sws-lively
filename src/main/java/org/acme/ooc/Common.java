package org.acme.ooc;

import static com.google.common.base.Objects.*;
import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.fao.sws.domain.Persistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Common {

	public static Logger log = LoggerFactory.getLogger("test");
	
	
	public static <T> void show(Collection<T> coll) {
		
		show(coll.stream());
	}
	
	public static <T> void show(Collection<T> coll, Function<T,?> mapfunc) {
		
		show(coll.stream(),mapfunc);
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
	
	public static <T extends Persistable> Predicate<T> otherThan(T thisone) {
		return t->!equal(t.getId(), thisone.getId());
	}
}
