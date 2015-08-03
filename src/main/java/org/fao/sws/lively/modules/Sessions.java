package org.fao.sws.lively.modules;

import static java.lang.String.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.fao.sws.domain.operational.Privilege.*;
import static org.fao.sws.lively.modules.Configuration.*;
import static org.fao.sws.lively.modules.Metadata.*;
import static org.fao.sws.lively.modules.Sessions.NewSessionClause.Density.*;
import static org.fao.sws.lively.modules.Users.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.UtilityClass;

import org.fao.sws.domain.plain.dataset.BaseDataValue;
import org.fao.sws.domain.plain.dataset.Metadata;
import org.fao.sws.domain.plain.dataset.Observation;
import org.fao.sws.domain.plain.dataset.ObservationCoordinates;
import org.fao.sws.domain.plain.dataset.SessionMetadata;
import org.fao.sws.domain.plain.dataset.SessionMetadataElement;
import org.fao.sws.domain.plain.dataset.SessionObservation;
import org.fao.sws.domain.plain.operational.EditingSession;
import org.fao.sws.domain.plain.operational.Pivoting;
import org.fao.sws.domain.plain.operational.User;
import org.fao.sws.domain.plain.reference.DataSet;
import org.fao.sws.domain.plain.reference.DimensionValue;
import org.fao.sws.domain.plain.reference.FlagValue;
import org.fao.sws.domain.plain.reference.MetadataType;
import org.fao.sws.ejb.EditingSessionService;
import org.fao.sws.ejb.ObservationService;
import org.fao.sws.ejb.SessionDataService;
import org.fao.sws.ejb.SessionObservationService;
import org.fao.sws.ejb.dto.SessionObservationDescriptor;
import org.fao.sws.lively.SwsTest;
import org.fao.sws.lively.modules.Sessions.NewSessionClause.Density;
import org.fao.sws.model.config.FlagConfiguration;
import org.fao.sws.model.dao.MetadataDao;
import org.fao.sws.model.dao.ObservationDao;
import org.fao.sws.model.dao.SessionMetadataDao;
import org.fao.sws.model.domain.SessionObservationDto;
import org.fao.sws.model.filter.DataPageFilter;
import org.fao.sws.model.filter.DataSetFilter;
import org.fao.sws.model.filter.ExtractionFilter;


@UtilityClass
public class Sessions extends Common {
	
				//inject dependencies in static class at startup
				void startup(@Observes SwsTest.Start e, 
														EditingSessionService sessions, 
														SessionDataService allobservations, 
														SessionObservationService uncommitted,
														ObservationService committedservice,
														ObservationDao committeddao,
														SessionMetadataDao sessionmetadatadao,
														MetadataDao metadatadao)	 {		
					
					Sessions.sessionservice = sessions;
					Sessions.sessionobservationservice = uncommitted;
					Sessions.sessionmetadatadao=sessionmetadatadao;	
					
					Sessions.observationdao = committeddao;
					Sessions.sessiondataservice=allobservations;
					Sessions.observationservice=committedservice;
					Sessions.metadatadao=metadatadao;	
					
				}		
				
				
		
	public EditingSessionService sessionservice;
	public SessionDataService sessiondataservice;
	public SessionObservationService sessionobservationservice;
	public SessionMetadataDao sessionmetadatadao;
	
	public ObservationDao observationdao;
	public ObservationService observationservice;
	public MetadataDao metadatadao;
	
	
	////////////  build  /////////////////////////////////////////////////////////////////////////////////////
	
	
	//// local DSL extension
	public interface NewSessionClause extends OverClause<DataSet,EditingSession>  {
		
		static final int defaultsize = 3;
		
		@AllArgsConstructor
		enum Density {LOW(.4),
					  MEDIUM(.7),
					  HIGH(.9);
					  @Getter  final double threshold;}
		
		/** the owning user (ovverrides the current user). **/
		NewSessionClause by(User user);
		
		/** the maximum number of coordinates for each dimension (overrides {@link #defaultsize}. **/
		NewSessionClause side(int side);
		
		NewSessionClause density(Density density);
		
		/** the underlying dataset, **/
		EditingSession committedOver(DataSet dataset);
		
	}
	
	
	/**
	 * Creates a session with random coordinates and random values.
	 */
	public NewSessionClause aNewSession() {
		
		
		
		return new NewSessionClause() {
			
			User user = currentuser;
			@Setter int side = defaultsize;
			@Setter Density density = MEDIUM;
			

			@Override public EditingSession over(@NonNull DataSet ds)  {
				
				sudo(()->{
					assign(READ,WRITE).to(oneof(groupsOf(user))).over(ds);
				});

				ExtractionFilter filter = new ExtractionFilter()
												.setDataSetCode(ds.getName())
												.setDomainCode(ds.getDomain().getCode())
												.setSessionDescription(random("session"))
												.setDimension2ids(aSelectionOver(ds,side));

			
				EditingSession session  = sessionservice.extract(filter,user);
				
				log.info("new {}({}) over ({} {})",session.getDescription(),
						   session.getId(),ds.getName(),
						   session.getDimension2ids().keySet());

				return session;
								
			}
			
			@Override
			public NewSessionClause by(User user) {
				this.user=user;
				return this;
			}
			
			@Override public EditingSession committedOver(DataSet dataset) {
				
				EditingSession session = over(dataset);
				
				randomlyFill(session, density);
				
				commit(session);
				
				return session;
			}
		};
		
	}
	
	
	// new observations
	
	//local DSL extension
	public interface NewObservationClause extends AtClause<List<Long>, SessionObservation>  {
		
		NewObservationClause withFlag(String name, String value);
		
		NewObservationClause value(BigDecimal value);
		
		SessionObservation thatShadows(Observation obs);
		
	}
	
	
	
	public NewObservationClause aNewObservationIn(EditingSession session) {
		
		return new NewObservationClause() {
		
			BigDecimal value = new BigDecimal(1 + (int)(Math.random()*10000));
			Map<String,FlagValue> flags = new HashMap<>();
			
			@Override
			public NewObservationClause value(BigDecimal v) {
				value = v;
				return this;
			}
			
			@Override
			public NewObservationClause withFlag(String name, String value) {
				flags.put(name,new FlagValue(value,random("flag")));
				return this;
			}
			
			@Override
			public SessionObservation at(List<Long> coords) {
				
				SessionObservation observation = new SessionObservation(session);
				observation.setValue(value);
				flags.forEach(observation::setFlag);
				observation.setDimensionIds(coords);
				
				return observation;
				
			}
			
			@Override
			public SessionObservation thatShadows(@NonNull Observation committed) {
				
				Observation fetched = observationdao.findById(filterFrom(session),committed.getId());
				
				List<Long> withoutnames = valuesIn(unfoldCoordinates(fetched.getDataPoint().getCode2dimension()));
				
				SessionObservation observation = at(withoutnames);
				
				observation.setValue(committed.getValue());
				observation.setCode2flag(new LinkedHashMap<>(committed.getCode2flag()));
				
				List<SessionMetadata> metadata = metadatadao.findByObservation(filterFrom(session),fetched)
											.stream()
											.map(m->convert(m,observation))
											.collect(toList());
				
				observation.setMetadata(metadata);
				
				return observation;
			}
			
		};
		
	}
	
	public Map<String,String> flagsOf(BaseDataValue<?,?>  observation) {
		
		Map<String,String> flags = new LinkedHashMap<>();
		
		observation.getCode2flag().forEach((k,v)->flags.put(k,v.getValue()));
		
		return flags;
	
	}


	
	
	
	//////////////// inspect //////////////////////////////////////////////////////////////////
	
	
	Predicate<SessionObservationDto> committedobservations = o->!o.isDirty();

	//local DSL extension
	public interface LookupClause<T> {
		
		/** The (first) observation that satisfies a given predicate.*/
		T lookup(Predicate<? super T> predicate);
		
		/** <code>true</code> if there is one observation that satisfies the predicate. */
		boolean exists(Predicate<? super T> predicate);
	}
	
	// lookup
	public <T extends BaseDataValue<?,?>> LookupClause<T>  given(Stream<T> observations) {
		
		return new LookupClause<T>() {
			
			@Override public T lookup(Predicate<? super T> predicate) {
				return oneof(observations.filter(predicate));
			}
			
			@Override public boolean exists(Predicate<? super T> predicate) {
				return observations.filter(predicate).findFirst().isPresent();
			}
		};
	}
	
	public Predicate<BaseDataValue<?,?>> withValue(@NonNull BigDecimal val) {
		return o->o.getValue().compareTo(val)==0;
	}
	
	public Predicate<Observation> at(@NonNull ObservationCoordinates coords) {
		return o->coords.getCode2dimension().equals(o.getDataPoint().getCode2dimension());
	}
	
	public Predicate<SessionObservation> at(@NonNull List<Long> coords) {
		return o->o.getDimensionIds().equals(coords);
	}
	
	public Predicate<BaseDataValue<?,?>> with(@NonNull Long id) {
		return o->o.getId().equals(id);
	}
	

	
	public void show(EditingSession session) {
		
		System.out.print("uncommitted\n");
		System.out.print("-----------\n");
		show(stagedIn(session),o->stateof(o));
		
		System.out.print("committed\n");
		System.out.print("---------\n");
		show(committedIn(session),o->stateof(o));

	}
	
	
	public String stateof(SessionObservation obs) {
		
		return format("staged:(%s) %s@%s v%s  %sMETA=%s",
									obs.getId(),
									obs.getValue(),
									obs.getDimensionIds(),
									obs.getVersion(),
									obs.getCode2flag().isEmpty()?"":format("FLAGS=%s",obs.getCode2flag()),
									obs.getMetadata());
	}
	
	
	public String stateof(Observation obs) {
		
		return format("committed:(%s) %s@%s v%s %sMETA=%s",
									   obs.getId(),
									   obs.getValue(),
									   valuesIn(unfoldCoordinates(obs.getDataPoint().getCode2dimension())),
									   obs.getVersion(),
									   obs.getCode2flag().isEmpty()?"":format("FLAGS=%s",obs.getCode2flag()),
									   obs.getMetadata());
	}
	

	
	public Predicate<BaseDataValue<?,?>> withoutMetadata = o->o.getMetadata().isEmpty();
	public Predicate<BaseDataValue<?,?>> withMetadata = withoutMetadata.negate();
	
	public Stream<Observation> committedIn(EditingSession session) {
		
		return committedIn(session,$->true);
	}
	
	public Stream<Observation> committedIn(EditingSession session, Predicate<Observation> predicate) {
		
		return observationsIn(session)
					.filter(committedobservations)
					.map(o -> {
		
						
						//dto lists coordinates in pivoting order, recreate named map here.
						Map<String,Long> coords = mapInPivotingOrder(o.getDimensionIds(),session);
						
						Observation obs = observationdao.findObservationHistoryByCoordinates(filterFrom(session),coords).get(0);
						
						//make this available to test, app doesn't.
						foldCoordinates(coords).forEach((k,v)->obs.getDataPoint().getCode2dimension().put(k,v));
						
						//make this available to test, app doesn't.
						obs.setMetadata(metadatadao.findByObservation(filterFrom(session), obs));
						
						return obs;
						
					})
					.filter(predicate);
	}
	
	public Stream<SessionObservation> stagedIn(EditingSession session) {
		
		return stagedIn(session,$->true);
	}
	
	/**
	 * The observations that have been saved to the session, but not yet committed.
	 */
	public Stream<SessionObservation> stagedIn(EditingSession session,Predicate<SessionObservation> predicate) {
		
		/*  observations are here physically separated and can be easily accessed directly. */
		
		Pivoting pivoting = configOf(session.getDataSet()).defaultPivoting();
		
		int size = sessionobservationservice.getSessionObservationCount(session);
		
		return sessionobservationservice.getSessionObservationPage(session,pivoting,1,size)
					.stream()
					.filter(predicate)
					.peek(o->{
						o.setMetadata(sessionmetadatadao.findBySessionObservation(o));
					});

	}
	
	
	
	public Stream<SessionObservationDto> observationsIn(EditingSession session) {
		
		/*	READTHIS:
		  
		  design tracks UI access patterns and extracts based on current layout (visualisation).
		  specifically, grouping columns are significant as a filter: 
		
		        "extracts observations with these coordinates for grouping dimensions".
		
		  web layer produces this 'subspace' based on "current page", we consider for whole session.

		*/
		
		//any pivoting would do, but default is readily shared.
		Pivoting pivoting = configOf(session.getDataSet()).defaultPivoting();
		
		//grouping values in this session.
		List<List<Long>> gselection = pivoting.getGroupingDimensions().stream()
												   .map(n->session.getDimension2ids().get(n.getCode()))
												   .collect(toList());
		
		//all points in the relevant subspace of grouping dimensions.
		List<List<Long>> gspace = pointsIn(gselection);
		
		DataPageFilter filter = new DataPageFilter().setPivoting(pivoting).setSession(session).setSubspace(gspace);
		
		return all(sessiondataservice.getDataPage(filter).values());
		
	}
	

	////////////////////// editing ///////////////
	
	
	/**
	 * Fills a session with random values.
	 */
	public void randomlyFill(EditingSession session, Density density) {
		
		/*	READTHIS:
		    
		    observations coordinates are unnamed, hence their ordering is significant.
		
			insertion in the database expects them to be in 'config order',
			i.e. how they appear in the configuration of a dataset's <observation> element.
			
			 unfortunately, config order is preserved in various domain objects via specific Map implementations,
			 i.e. implicitly. Here we find it preserved in the session.
			
			all this is rather brittle and tucked away, imo. 
			we document here it in case something starts breaking later.
		*/
		
		List<List<Long>> sessioncube = new ArrayList<>(session.getDimension2ids().values());
		
		List<List<Long>> points = pointsIn(sessioncube);
		
		points.stream().forEach(point->addObservationTo(session,point));
		
		log.info("saved {} observations in {}",points.size(),session.getDescription());
		
	}
	
	/**
	 * Persists changes to session (without committing them).
	 */
	public void saveAndCommit(SessionObservation observation) {
		saveValueAndFlagsOf(observation);
		saveMetadataOf(observation);
		commit(observation.getEditingSession());
	}
	
	/**
	 * Persists value/falg changes to session (without committing them).
	 */
	public void saveValueAndFlagsOf(SessionObservation observation) {
		
		//save value/flags
		sessionobservationservice.updateMultipleValues(singletonList(observation));
	}
	
	/**
	 * Persists metadata changes to session (without committing them).
	 */
	public void saveMetadataOf(SessionObservation observation) {
		
		EditingSession session = observation.getEditingSession();
	
		Iterator<Long> it = observation.getDimensionIds().iterator();
		Map<String,Long> coords = new LinkedHashMap<>();
		session.getDimension2ids().forEach((k,v)->coords.put(k,it.next()));
		
		
		SessionObservationDescriptor descriptor = new SessionObservationDescriptor(session, 
																				   coords, 
																				   observation.getId(), 
																				   observation.getVersion());
		
		SessionObservation updated = sessionobservationservice.updateMetadata(descriptor, observation.getMetadata());
		
		observation.setId(updated.getId());
		observation.setVersion(updated.getVersion());
		
	}
	
	/**
	 * Save staged observations in a given session.
	 */
	public void commit(EditingSession session) {
		
		sessionservice.transferToMain(session,null,null); //no conflicts here to worry about.
		
		log.info("committed changes in {}",session.getDescription());
	}
	
	
	public SessionMetadata aNewMetadataFor(SessionObservation observation) {
		
		MetadataType type = aMetadataType();
		
		SessionMetadata metadata = new SessionMetadata(null, type, aLanguage(), observation);
		
		List<SessionMetadataElement> elements = IntStream.range(1,randomBetween(2,3)) //at least one element,at most two
												.mapToObj(i->aNewMetadataElementFor(metadata))
												.collect(toList());
		
		
		metadata.setElements(elements);
		
		return metadata;
	}
	
	
	
	public SessionMetadataElement aNewMetadataElementFor(SessionMetadata metadata) {
		
		return new SessionMetadataElement(null,"...", aMetadataElementTypeOf(metadata.getType()) , metadata);
		
	}
	
	//////////////// helpers /////////////////////////////////////////////////////////////////////////////////////////////
	

	/** A random selection of a given 'side' over a given dataset. */
	private Map<String,List<Long>> aSelectionOver(DataSet ds, int side) {
		
		Map<String,List<Long>> selection = new HashMap<>();
		
		dimensionsOf(ds).forEach(dim-> {
			
			Stream<Long> coords = shuffle(all(valuesOf(dim))).filter(leaves).limit(side).map(coordinateids);
		
			selection.put(dim.getCode(), all(coords));
		});
		
		return selection;	
	}
	
	public List<List<Long>> pointsIn(EditingSession session) {
		return pointsIn(new ArrayList<>(session.getDimension2ids().values()));
	}
	
	/** Data points inside a selection. */
	private List<List<Long>> pointsIn(List<List<Long>> selection) {
		
		//inefficient but clear: good for testing.
		
		Stream<Long> head = selection.get(0).stream();
		
		int cardinality = selection.size();
		
		List<List<Long>> allcoords = new ArrayList<>();
		
		
		if (cardinality==1)
			
			//base case: unidimensional selection
			
			head.forEach(c->allcoords.add(singletonList(c)));
		
		else {
		
			//recursive step: solve for less dimensions then add up and increase dimensionality (2d to 3d to ...) 
			List<List<Long>> subselection = selection.subList(1,cardinality); //one-dimension less.
			
			head.forEach(value->{
				
				pointsIn(subselection).forEach(point->{
					
					List<Long> slice = new ArrayList<>(point.size()+1);
					slice.add(value);
					slice.addAll(point);
					allcoords.add(slice);
				});
				
			});
		}
		
		return allcoords;	
				
	}
	
	
	private DataSetFilter filterFrom(EditingSession session) {
		
		return new DataSetFilter().setDataSetCode(session.getDataSet().getName())
								  .setDomainCode(session.getDataSet()
								  .getDomain().getCode());
	}
	
	
	private Map<String,Long> unfoldCoordinates(Map<String,DimensionValue> coords) {
		
		Map<String,Long> converted = new LinkedHashMap<>();
		coords.forEach((k,v)->converted.put(k,v.getId()));
		return converted;
		
	}
	
	
	private Map<String,DimensionValue> foldCoordinates(Map<String,Long> coords) {
		
		Map<String,DimensionValue> converted = new LinkedHashMap<>();
		coords.forEach((k,v)->converted.put(k,new DimensionValue(v)));
		return converted;
		
	}
	
	private <T> List<T> valuesIn(Map<String,T> coords) {
		
		return new ArrayList<>(coords.values());
		
	}

	private Map<String,Long> mapInPivotingOrder(List<Long> coords,EditingSession session) {
		
		Pivoting pivoting = configOf(session.getDataSet()).defaultPivoting();
		
		Map<String,Long> named = new LinkedHashMap<>();
		
		Iterator<Long> values = coords.iterator();
		
		pivoting.toDimensionList().forEach(pdim->named.put(pdim.getCode(),values.next()));
		
		return mapInConfigOrder(named, session);
	}
	
	private Map<String,Long> mapInConfigOrder(Map<String,Long> coords,EditingSession session) {
		
		Map<String,Long> converted = new LinkedHashMap<>();
		
		session.getDimension2ids().forEach((k,v)->converted.put(k,coords.get(k)));
		
		return converted;
	}
	
	
	private SessionObservation addObservationTo(EditingSession session, List<Long> point) {
		
		FlagConfiguration flag = randomIn(all(Configuration.flagsOf(session.getDataSet())));
		
		SessionObservation observation = aNewObservationIn(session).at(point);
		
		if (flag!=null) 
			observation.setFlag(flag.getCode(),new FlagValue(oneof(valuesOf(flag)),random("flag")));
		
		saveValueAndFlagsOf(observation);
		
		List<SessionMetadata> metadata = generateMetadataFor(observation);
		
		observation.setMetadata(metadata);
		
		saveMetadataOf(observation);
		
		return observation;
	}
	
	private List<SessionMetadata> generateMetadataFor(SessionObservation observation) {
		
		return IntStream.range(0,randomBetween(2,4)) //sometimes, no metadata at all.
								.mapToObj(i->aNewMetadataFor(observation))
								.collect(toList());
		
	}

	
	private SessionMetadata convert(Metadata metadata, SessionObservation obs) {
		
		SessionMetadata smetadata = new SessionMetadata(null,metadata.getType(),metadata.getLanguage(),obs);
		
		List<SessionMetadataElement> melements = metadata.getElements()
													.stream()
													.map(me->new SessionMetadataElement(null,me.getValue(), me.getType(), smetadata))
													.collect(toList());
		
		smetadata.setElements(melements);
		return smetadata;
	}
	
	///public static void main(String[] args) {}
}