package org.fao.sws.lively.modules;

import static com.google.common.base.Objects.*;
import static java.lang.String.*;
import static java.util.UUID.*;
import static java.util.stream.Collectors.*;
import static java.util.stream.IntStream.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;

import org.fao.sws.domain.plain.traits.Persistable;
import org.fao.sws.model.config.Describable;
import org.fao.sws.web.dto.ResponseWrapperSingleObj;
import org.junit.Assert;

/**
 * A potpourri of general-purpose idioms available for static import. 
 */
public class Common extends DomainModule {
	
	
	
	/** a <code>true</code> alias, with gusto. */
	public static boolean yep = true;
	
	/** a <code>false</code> alias, with gusto. */
	public static boolean nope = false;
	

	////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/** pulls out those identifiers. */
	public static Function<Persistable,Long> ids = p->p.getId();  
	
	/** pulls out those codes. */
	public static Function<Describable,String> codes = d->d.getCode();
	
	
	/** shows it. */
	public static <T> void show(Object text) { //just for consistency and looks.
		
		System.out.println(text);
	}

	/** shows them. */
	public static <T> void show(@NonNull Stream<T> stream) {	
		show(stream, Function.identity());
	}
	
	/** shows the interesting bits.*/
	public static <T> void show(@NonNull Stream<T> stream, @NonNull Function<? super T,?> mapfunc) {
		System.out.println(format("total=%s",stream.map(mapfunc).peek(e->System.out.println(e)).count()));
	}
	
	
	/** extracts identifiers. */
	public static <T extends Persistable> Stream<Long> idsOf(@NonNull Stream<T> identifiables) {
		return identifiables.map(ids);
	}

	/** extracts codes. */
	public static <T  extends Describable> Stream<String> codesOf(@NonNull Stream<T> describables) {
		return describables.map(codes);
	}

	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/** the first element. */
	public static <T> T oneof(@NonNull Stream<T> stream) {
		return stream.findFirst().orElseThrow(()->new IllegalStateException("no such element"));
	}
	
	/** dedups, based on codes. */
	public static <T extends Describable> Stream<T> distinct(@NonNull Stream<T> describables) {
		
		List<String> codes = new ArrayList<String>();
		
		return describables.filter(d->!codes.contains(d.getCode())).peek(d->codes.add(d.getCode()));
	}
	
	/** extracts any element, except that very one. */
	public static <T> ExceptClause<T,T> anyIn(@NonNull Stream<T> stream) {
		return t1->oneof(stream.filter(t2->!t1.equals(t2)));
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/** accumulates it. */
	public static <T> List<T> all(@NonNull Stream<T> stream) {
		return stream.collect(toList());
	}
	
	/** streams it out. */
	public static <T> Stream<T> all(@NonNull Collection<T> coll) {
		return coll.stream();
	}
	
	/** invents a name (with a meaningful prefix) */
	public static String random(@NonNull String prefix) {
		return format("test%s-%s",prefix,randomUUID());
	}
	
	/**
	 * picks one in the middle of two, inclusive.
	 */
	public static int randomBetween(int from, int to) {
		
		return from + (int) (Math.random()*(to-from+1));
	}

	/** pick an element at random. */
	public static <T> T randomIn(@NonNull Collection<T> coll) {
		
		return coll.isEmpty() ? null : all(coll).skip(randomBetween(0,coll.size()-1)).findFirst().get();
	}
	
	/** scrambles the elements for maximum chaos. */
	public static <T> Stream<T> shuffle(@NonNull Collection<T> coll) {
		
		List<T> aslist = new ArrayList<T>(coll);
		
		List<Integer> indices = range(0,coll.size()).boxed().collect(toList());
		
		Collections.shuffle(indices);
		
		return indices.stream().map(aslist::get);
			
	}
	
	/** scrambles the elements for maximum chaos. */
	public static <T> Stream<T> shuffle(@NonNull Stream<T> stream) {
		
		return shuffle(all(stream));
			
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/** ensures diversity (by id). **/
	public static <T extends Persistable> Predicate<T> isSameAs(@NonNull T thisone) {
		return t->equal(t.getId(), thisone.getId());
	}
	
	public static <T extends Persistable> Predicate<T> isOtherThan(@NonNull T thisone) {
		return isSameAs(thisone).negate();
	}
	
	/** ensures diversity (by code). **/
	public static <T extends Describable> Predicate<T> isSameAs(@NonNull T thisone) {
		return t->equal(t.getCode(), thisone.getCode());
	}
	
	/** ensures diversity (by code). **/
	public static <T extends Describable> Predicate<T> isOtherThan(@NonNull T thisone) {
		return isSameAs(thisone).negate();
	}
	
	
		
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	/** assign it to what you expect it to be. */
	public static <T> T singleobjectIn(@NonNull Response response) {
		
		ResponseWrapperSingleObj<T> single  = payloadIn(response);
		return single.getResult();
	}
	
	/** assign it to what you expect it to be. */
	@SuppressWarnings("all")
	public static <T> T payloadIn(@NonNull Response response) { // wildcard capture for fluent assignments
		return (T) response.getEntity();
	}
	
	/** consumes the stream therein.*/
	@SneakyThrows
	public static void consume(@NonNull Response response) {
		
		StreamingOutput stream =  payloadIn(response);
		
		@Cleanup
		OutputStream out = new OutputStream() {
			
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
		};
		
		stream.write(out);
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/** like junit's, uses identifiers. */
	public static void assertEquals(@NonNull String message, @NonNull Persistable t1, @NonNull Persistable t2) {
		Assert.assertEquals(message,t1.getId(), t2.getId());
	}
	
	//helps with static imports from subclass
	public static void assertEquals(@NonNull String message, @NonNull Object t1, @NonNull Object t2) {
		Assert.assertEquals(message,t1, t2);
	}
	
	/** like junit's, uses identifiers. */
	public static void assertEquals(@NonNull Persistable t1, @NonNull Persistable t2) {
		Assert.assertEquals(t1.getId(), t2.getId());
	}
	
	/** like junit's, uses codes. */
	public static void assertEquals(@NonNull String message, @NonNull Describable t1, @NonNull Describable t2) {
		Assert.assertEquals(message, t1.getCode(), t2.getCode());
	}

	/** like junit's, uses codes. */
	public static void assertEquals(@NonNull Describable t1, @NonNull Describable t2) {
		Assert.assertEquals(t1.getCode(), t2.getCode());
	}
	
	/** like junit's, says it clearly for a stream. */
	public static void assertThereAreNo(@NonNull String message, @NonNull Stream<?> t) {
		
		assertTrue(message,t.count()==0);
	}
	
	//helps with static imports from subclass
	public static void assertEquals(@NonNull Object t1, @NonNull Object t2) {
		Assert.assertEquals(t1, t2);
	}
	
	/** like junit's, says it clearly for a stream. */
	public static void assertThereAreNo(@NonNull Stream<?> t) {
		assertTrue(t.count()==0);
	}

	/** like junit's, says it clearly for a map. */
	public static void assertThereAreNo(@NonNull Map<?,?> t) {
		assertTrue(t.isEmpty());
	}
	
	/** like junit's, says it clearly for a stream. */
	public static void assertThereAre(String msg, Stream<?> t) {
		assertTrue(msg, t.count()>0);
	}
	
	/** like junit's, says it clearly for a stream. */
	public static void assertThereAre(Stream<?> t) {
		assertTrue(t.count()>0);
	}
	
	
		
}
