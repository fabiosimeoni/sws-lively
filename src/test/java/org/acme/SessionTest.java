package org.acme;

import static org.fao.sws.lively.modules.Common.*;
import static org.fao.sws.lively.modules.Configuration.*;
import static org.fao.sws.lively.modules.Sessions.*;
import static org.fao.sws.lively.modules.Sessions.NewSessionClause.Density.*;
import static org.fao.sws.lively.modules.Users.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.fao.sws.domain.plain.dataset.Observation;
import org.fao.sws.domain.plain.dataset.SessionObservation;
import org.fao.sws.domain.plain.operational.EditingSession;
import org.fao.sws.domain.plain.operational.User;
import org.fao.sws.lively.SwsTest;
import org.fao.sws.lively.modules.Sessions.NewSessionClause.Density;
import org.junit.Test;

public class SessionTest extends SwsTest {

	
	@Test
	public void default_owner_is_current_user() {
		
		EditingSession session = aNewSession().over(aDataset());
		
		assertEquals(currentuser,session.getUser());
		
	}
	
	@Test
	public void owner_can_be_customised() {
		
		EditingSession session = aNewSession().over(aDataset());
		
		User user = aRegularUser();
		
		session = aNewSession().by(user).over(aDataset());
		
		assertEquals(user,session.getUser());
		
	}
	
	@Test
	public void size_can_be_customised() {
		
		
		EditingSession session = aNewSession().side(5).over(aDataset());
		
		int dimensionality = session.getDimension2ids().keySet().size();
		
		assertTrue("size is bound by dimensionality and size",
				
				pointsIn(session).size()<=Math.pow(5,dimensionality)
				
		);
	}
	
	@Test
	public void density_can_be_customised() {
		
		EditingSession sessionA = aNewSession().side(5).density(LOW).over(aDataset());
		EditingSession sessionB = aNewSession().side(5).density(HIGH).over(sessionA.getDataSet());
		
		assertTrue("low density sessions have less points than high density sessions",
				
				pointsIn(sessionA).size() < pointsIn(sessionB).size()
		);
	}
	
	@Test
	public void sessions_can_start_empty() {
		
		EditingSession session = aNewSession().over(aDataset());
		
		assertThereAreNo(stagedIn(session));
		
	}
	
	
	
	@Test
	public void session_support() {
		
		EditingSession session = aNewSession().over(aDataset());
		
		assertEquals(currentuser,session.getUser());
		
		session = aNewSession().by(aRegularUser()).side(2).over(aDataset());
		
		assertThereAreNo(stagedIn(session));
		
		randomlyFill(session, Density.HIGH);
		
		assertThereAre(stagedIn(session));
		
		//simulated editing...
		SessionObservation observation = oneof(stagedIn(session));
		
		show(stateof(observation));
		
		observation = given(stagedIn(session)).lookup(with(observation.getId()));
		
		show(stateof(observation));
		
		observation.setValue(new BigDecimal(99));

		saveValueAndFlagsOf(observation);
		
		show(stateof(observation));
		
		// lookup by coords
		SessionObservation fetched = given(stagedIn(session)).lookup(at(observation.getDimensionIds()));
		
		show(stateof(fetched)); //still transient
		
		fetched = given(stagedIn(session)).lookup(at(observation.getDimensionIds()));
		
		assertEquals(observation,fetched);
		
		show(session);
		
		//lookup by value
		fetched = given(stagedIn(session)).lookup(withValue(new BigDecimal(99)));
		
		assertEquals(observation,fetched);
		
		commit(session);
		
		assertThereAre(committedIn(session));
		
		Observation committed = oneof(committedIn(session));
		
		//lookup by id.
		Observation fetchedCommitted = given(committedIn(session)).lookup(with(committed.getId()));
		
		assertEquals(committed,fetchedCommitted);
		
		fetchedCommitted = given(committedIn(session)).lookup(at(committed.getDataPoint()));
		
		assertEquals(committed,fetchedCommitted);
		
		

	}
}
