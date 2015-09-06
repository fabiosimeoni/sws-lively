package org.acme.sessions;

import static java.math.BigDecimal.*;
import static org.fao.sws.lively.modules.Configuration.*;
import static org.fao.sws.lively.modules.Sessions.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.fao.sws.domain.plain.dataset.Observation;
import org.fao.sws.domain.plain.dataset.SessionMetadata;
import org.fao.sws.domain.plain.dataset.SessionMetadataElement;
import org.fao.sws.domain.plain.dataset.SessionObservation;
import org.fao.sws.domain.plain.operational.EditingSession;
import org.fao.sws.domain.plain.reference.FlagValue;
import org.fao.sws.lively.SwsTest;
import org.junit.Test;

public class SessionEditing extends SwsTest {

	@Test
	public void updates_commit() {
		
		EditingSession session = aNewSession().committedOver(aDataset());
		
		Observation committed = oneof(committedIn(session));
		
		//remember
		Long version = committed.getVersion(); 
		BigDecimal value = committed.getValue();
		int metadatasize = committed.getMetadata().size();
		
		SessionObservation changed = aNewObservationIn(session).thatShadows(committed);
		
		changed.setValue(committed.getValue().add(ONE));
		changed.getMetadata().add(aNewMetadataFor(changed));
		
		saveAndCommit(changed);
		
		committed = given(committedIn(session)).lookup(at(committed.getDataPoint()));
		
		assertTrue("version has grown",committed.getVersion()>version);	
		assertTrue("value has grown",committed.getValue().compareTo(value)>0);	
		assertTrue("metadata has grown",committed.getMetadata().size()>metadatasize);
	}
	
	@Test
	public void metadataonly_updates_commit() {
		
		EditingSession session = aNewSession().committedOver(aDataset());
		
		Observation committed = oneof(committedIn(session));
		
		SessionObservation changed = aNewObservationIn(session).thatShadows(committed);
		
		assertNull("has no id",changed.getId());
		assertNull("has no version",changed.getVersion());
		
		changed.getMetadata().add(aNewMetadataFor(changed));

		saveMetadataOf(changed);

		Long id = changed.getId();
		Long version = changed.getVersion();

		assertNotNull("has an id",id);
		assertNotNull("has a version",version);
		assertTrue("has been saved",given(stagedIn(session)).exists(with(changed.getId())));
		
		//retrieve
		changed = given(stagedIn(session)).lookup(with(changed.getId()));
		
		assertEquals("has same id",id,changed.getId());
		assertEquals("has same version",version,changed.getVersion());
		
		//apply further changes  
		changed.getMetadata().add(aNewMetadataFor(changed));
		
		saveMetadataOf(changed);
		
		assertEquals("id remains stable",id, changed.getId());
		assertTrue("version is up",changed.getVersion()>version);
		
		commit(session);
		
		Observation latest = given(committedIn(session)).lookup(at(committed.getDataPoint()));
		
		assertNotEquals("a new observation has been committed",latest.getId(), committed.getId());
		assertTrue("new observation has a greater version",latest.getVersion()>committed.getVersion());
		assertTrue("new observation has more metadata",	latest.getMetadata().size()>committed.getMetadata().size());

	}
	
	
	
	@Test
	public void phantom_metadata_updates_are_discarded() {
		
		
		EditingSession session = aNewSession().committedOver(aDataset());
		
		
		//stage an observation with metadata
		Observation committed = oneof(committedIn(session).filter(withMetadata));
		
		//case_add_and_remove: 
		
		{
		
			SessionObservation changed = aNewObservationIn(session).thatShadows(committed);
			
			SessionMetadata metadata = aNewMetadataFor(changed);
			
			changed.getMetadata().add(metadata); //change
			
			int index = changed.getMetadata().lastIndexOf(metadata); //remember to remove
			
			saveMetadataOf(changed);
			
			assertTrue("is in session", given(stagedIn(session)).exists(at(changed.getDimensionIds())));
			
			changed.getMetadata().remove(index); //revert change
			
			saveMetadataOf(changed);
			
			assertFalse("is no longer in session", given(stagedIn(session)).exists(at(changed.getDimensionIds())));
			
		}
		
		

		// case_remove_and_add: 
		{
			
			SessionObservation changed = aNewObservationIn(session).thatShadows(committed);
			
			SessionMetadata metadata = changed.getMetadata().get(0);
			
			changed = aNewObservationIn(session).thatShadows(committed);
			
			changed.getMetadata().remove(0);  //change

			saveMetadataOf(changed);
			
			assertTrue("is in session", given(stagedIn(session)).exists(at(changed.getDimensionIds())));
			
			changed.getMetadata().add(metadata);
			
			saveMetadataOf(changed);
			
			assertFalse("is no longer session", given(stagedIn(session)).exists(at(changed.getDimensionIds())));
		
		}
		
		// case_add_element: 
		{
			
			SessionObservation changed = aNewObservationIn(session).thatShadows(committed);
			
			SessionMetadata metadata = changed.getMetadata().get(0);
			
			SessionMetadataElement element = aNewMetadataElementFor(metadata);
			
			metadata.getElements().add(element);
			
			saveMetadataOf(changed);
			
			assertTrue("is in session", given(stagedIn(session)).exists(at(changed.getDimensionIds())));
			
			metadata.getElements().remove(element);
			
			saveMetadataOf(changed);
			
			assertFalse("is no longer session", given(stagedIn(session)).exists(at(changed.getDimensionIds())));
		}
		
		// case_edit_element: 
		{
			
			SessionObservation changed = aNewObservationIn(session).thatShadows(committed);
			
			SessionMetadata metadata = changed.getMetadata().get(0);
			
			SessionMetadataElement element = metadata.getElements().get(0);
			
			String value = element.getValue(); //remember
			
			element.setValue("changed");
			
			saveMetadataOf(changed);
			
			assertTrue("is in session", given(stagedIn(session)).exists(at(changed.getDimensionIds())));
			
			element.setValue(value);
			
			saveMetadataOf(changed);
			
			assertFalse("is no longer session", given(stagedIn(session)).exists(at(changed.getDimensionIds())));
		}

	}
	
	
	
	@Test
	public void sessions_retain_observations_only_if_they_are_effective_updates() {
		
		EditingSession session = aNewSession().committedOver(aDatasetThat(hasFlags));
		
		show(session);
		
		Observation committed = oneof(committedIn(session));
		
		SessionObservation changed = aNewObservationIn(session).thatShadows(committed);
		
		//change value
		changed.setValue(changed.getValue().add(ONE));
		
		//change all flags
		flagsOf(changed).forEach((k,v)->
			changed.setFlag(k, new FlagValue(anyIn(valuesOf(flag(k))).except(v),random("flag")))	
		);
		
		Long versionbefore = changed.getVersion(); //is null before first save
		
		saveValueAndFlagsOf(changed);
		
		assertTrue("has a new version", changed.getVersion() != versionbefore);
		
		//effectively restore committed values
		changed.setValue(committed.getValue());
		changed.setCode2flag(committed.getCode2flag());
		
		versionbefore = changed.getVersion();
		
		saveValueAndFlagsOf(changed);
		
		assertNull("has no identifier",changed.getId());
		
		assertNull("has no version",changed.getVersion());
		
		assertFalse("is no longer in session",given(stagedIn(session)).exists(at(changed.getDimensionIds())));

		//confirms further that nothing has been committed.
		commit(session);
		
		Observation fetched = given(committedIn(session)).lookup(with(committed.getId()));
		
		assertSame("still has the same version",committed.getVersion(),fetched.getVersion());
		
		//can continue to update afterwards?
		
		//change value
		changed.setValue(changed.getValue().add(ONE));
		
		saveValueAndFlagsOf(changed);
		
		assertNotNull("has a version",changed.getVersion());
		
		commit(session);
		
	}
	
	
	
//	@Test
//	public void sessions_retain_new_observations_only_if_they_are_effective_updates() {
//		
//		EditingSession session = aSession().committedOver(aDataset());
//		
//		Observation committed = oneof(committedIn(session));
//		
//		show(session);
//		
//		delete(committed).from(session);
//		
//		show(session);
//		
//		assertFalse(given(committedIn(session)).exists(at(committed.getDataPoint())));

//		commi
//		SessionObservation newone = aNewObservationIn(session).at(committed.)
//				
//
//				
//		Long currentversion = committed.getVersion(); //remember
//		
//		SessionObservation changed = aNewObservationIn(session).toUpdate(committed);
//		
//		changed.setValue(committed.getValue().add(ONE));
//		
//		save(changed);
//		
//		SessionObservation fetched = given(uncommittedIn(session)).lookup(at(changed.getDimensionIds()));
//		
//		assertTrue(changed.getValue().doubleValue()==fetched.getValue().doubleValue());
//		
//		changed.setValue(committed.getValue().subtract(ONE));
//		
//		save(changed);
//		
//		boolean stilltocommit = given(uncommittedIn(session)).exists(at(changed.getDimensionIds()));
//		
//		assertFalse(stilltocommit);
//		
//		//confirms further that nothing has been committed.
//		commit(session);
//		
//		committed = given(committedIn(session)).lookup(withId(committed.getId()));
//		
//		assertSame(currentversion,committed.getVersion());
//	}
	
}
