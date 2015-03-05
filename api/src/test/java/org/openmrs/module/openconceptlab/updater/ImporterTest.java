package org.openmrs.module.openconceptlab.updater;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclConcept.Description;
import org.openmrs.module.openconceptlab.client.OclConcept.Name;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class ImporterTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	Importer importer;
	
	@Autowired
	@Qualifier("conceptService")
	ConceptService conceptService;
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies save new concept
	 */
	@Test
	public void importConcept_shouldSaveNewConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		importer.importConcept(null, oclConcept);
		assertImported(oclConcept);
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies add new names to concept
	 */
	@Test
	public void importConcept_shouldAddNewNamesToConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		importer.importConcept(null, oclConcept);
		
		Name thirdName = new Name();
		thirdName.setName("Third name");
		thirdName.setLocale(new Locale("pl", "PL"));
		thirdName.setLocalePreferred(true);
		thirdName.setNameType(ConceptNameType.FULLY_SPECIFIED.toString());
		oclConcept.getNames().add(thirdName);
		
		Name fourthName = newFourthName();
		oclConcept.getNames().add(fourthName);
		
		importer.importConcept(null, oclConcept);
		assertImported(oclConcept);
	}

	private Name newFourthName() {
	    Name fourthName = new Name();
		fourthName.setName("Fourth name");
		fourthName.setLocale(Context.getLocale());
		fourthName.setLocalePreferred(false);
		fourthName.setNameType(ConceptNameType.SHORT.toString());
	    return fourthName;
    }
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies update name type in concept
	 */
	@Test
	public void importConcept_shouldUpdateNameTypeInConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		importer.importConcept(null, oclConcept);
		
		for (Name name : oclConcept.getNames()) {
	        if (name.getNameType() == null) {
	        	name.setNameType(ConceptNameType.SHORT.toString());
	        }
        }
		
		importer.importConcept(null, oclConcept);
		assertImported(oclConcept);		
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies void names from concept
	 */
	@Test
	public void importConcept_shouldVoidNamesFromConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		Name fourthName = newFourthName();
		oclConcept.getNames().add(fourthName);
		importer.importConcept(null, oclConcept);
		
		List<Name> voided = new ArrayList<OclConcept.Name>();
		for (Iterator<Name> it = oclConcept.getNames().iterator(); it.hasNext();) {
	        Name name = it.next();
	        if (!name.isLocalePreferred()) {
	        	it.remove();
	        	voided.add(name);
	        }
        }
		assertThat(voided, is(not(empty())));
		
		importer.importConcept(null, oclConcept);
		Concept concept = assertImported(oclConcept);
		
		Collection<ConceptName> nonVoidedNames = concept.getNames(false);
		Collection<ConceptName> voidedNames = new ArrayList<ConceptName>(concept.getNames(true));
		voidedNames.removeAll(nonVoidedNames);
		assertThat(voidedNames, containsNamesInAnyOrder(voided));
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies add new descriptions to concept
	 */
	@Test
	public void importConcept_shouldAddNewDescriptionsToConcept() throws Exception {

		OclConcept oclConcept = newOclConcept();
	    importer.importConcept(null, oclConcept);

		Description desc1 = new Description();
		desc1.setDescription("test oclConceptDescription");
		desc1.setLocale(Context.getLocale());
		oclConcept.getDescriptons().add(desc1);

		importer.importConcept(null, oclConcept);

		assertImported(oclConcept);

	}

	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies void descriptions from concept
	 */
	@Test
	public void importConcept_shouldVoidDescriptionsFromConcept() throws Exception {

		OclConcept oclConcept = newOclConcept();
		importer.importConcept(null, oclConcept);

		Description desc1 = new Description();
		desc1.setDescription("test oclConceptDescription");
		desc1.setLocale(Context.getLocale());
		oclConcept.getDescriptons().add(desc1);

		importer.importConcept(null, oclConcept);
		Concept concept = assertImported(oclConcept);

		//cloning object to save state of descriptions after importing again
		Concept cloned = (Concept)org.apache.commons.lang.SerializationUtils.clone(concept);

		Collection<ConceptDescription> descriptionsBeforeVoiding = cloned.getDescriptions();

		List<Description> voided = new ArrayList<OclConcept.Description>();
		for (Iterator<Description> it = oclConcept.getDescriptons().iterator(); it.hasNext();) {
			Description description = it.next();
			if(description.getDescription().equals(desc1.getDescription())) {
				it.remove();
				voided.add(description);
			}
		}
		assertThat(voided, is(not(empty())));

		//at this point without cloning object original desc collecion is lost
		importer.importConcept(null, oclConcept);
	    concept = assertImported(oclConcept);

		final Collection<ConceptDescription> remainingDescriptions = concept.getDescriptions();
		/*
		it's equivalent for descriptionsBeforeVoiding.removeAll(remoiningDescriptions) which can't work becouse of refs disagreement
		it filters descriptionsBeforeVoiding.getDescription() is compared with remainingDescriptions.getDescription()
		*/
		Collection<ConceptDescription> recievedVoidedDescriptions = CustomPredicate.filter(descriptionsBeforeVoiding,
				new IPredicate<ConceptDescription>() {
					public boolean apply(ConceptDescription objectOfA) {
						CustomPredicate.predicateParams = objectOfA.getDescription();
						return CustomPredicate.select(remainingDescriptions, new IPredicate<ConceptDescription>() {
							public boolean apply(ConceptDescription objectOfB) {
								return objectOfB.getDescription().equals(CustomPredicate.predicateParams.toString());
							}
						}) == null;
					}
				});

		assertThat(recievedVoidedDescriptions, containsDescriptionsInAnyOrder(voided));

	}

	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies retire concept
	 */
	@Test
	public void importConcept_shouldRetireConcept() throws Exception {

		OclConcept oclConcept = newOclConcept();
		assertFalse(oclConcept.isRetired());
		importer.importConcept(null, oclConcept);

		oclConcept.setRetired(true);

		importer.importConcept(null, oclConcept);

		Concept concept = assertImported(oclConcept);
		assertTrue(concept.isRetired());
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies unretire concept
	 */
	@Test
	public void importConcept_shouldUnretireConcept() throws Exception {

		OclConcept oclConcept = newOclConcept();
		oclConcept.setRetired(true);
		assertTrue(oclConcept.isRetired());
		importer.importConcept(null, oclConcept);

		oclConcept.setRetired(false);

		importer.importConcept(null, oclConcept);

		Concept concept = assertImported(oclConcept);
		assertFalse(concept.isRetired());

	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies update datatype
	 */
	@Test
	public void importConcept_shouldUpdateDatatype() throws Exception {
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies update concept class
	 */
	@Test
	public void importConcept_shouldUpdateConceptClass() throws Exception {
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies fail if concept class missing
	 */
	@Test
	public void importConcept_shouldFailIfConceptClassMissing() throws Exception {
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies fail if datatype missing
	 */
	@Test
	public void importConcept_shouldFailIfDatatypeMissing() throws Exception {
	}
	
	public OclConcept newOclConcept() {
		OclConcept oclConcept = new OclConcept();
		
		oclConcept.setUuid("6c1bbb30-55f6-11e4-8ed6-0800200c9a66");
		
		oclConcept.setConceptClass("Test");
		oclConcept.setDatatype("N/A");
		oclConcept.setDateCreated(new Date());
		oclConcept.setDateUpdated(new Date());
		
		List<Description> descriptons = new ArrayList<OclConcept.Description>();
		Description description = new Description();
		description.setDescription("Test description");
		description.setLocale(Context.getLocale());
		descriptons.add(description);
		oclConcept.setDescriptons(descriptons);
		
		List<Name> names = new ArrayList<OclConcept.Name>();
		Name name = new Name();
		name.setName("Test name");
		name.setLocale(Context.getLocale());
		name.setLocalePreferred(true);
		name.setNameType(ConceptNameType.FULLY_SPECIFIED.toString());
		names.add(name);
		
		Name secondName = new Name();
		secondName.setName("Second name");
		secondName.setLocale(Context.getLocale());
		secondName.setLocalePreferred(false);
		names.add(secondName);
		
		oclConcept.setNames(names);
		
		return oclConcept;
	}
	
	private Concept assertImported(OclConcept oclConcept) {
		Concept concept = conceptService.getConceptByUuid(oclConcept.getUuid());
		assertThat(concept, is(notNullValue()));
		
		ConceptClass conceptClass = conceptService.getConceptClassByName(oclConcept.getConceptClass());
		assertThat(concept.getConceptClass(), is(conceptClass));
		
		ConceptDatatype conceptDatatype = conceptService.getConceptDatatypeByName(oclConcept.getDatatype());
		assertThat(concept.getDatatype(), is(conceptDatatype));
		assertThat(concept.getNames(false), containsNamesInAnyOrder(oclConcept.getNames()));
		assertThat(concept.getDescriptions(), containsDescriptionsInAnyOrder(oclConcept.getDescriptons()));
		
		return concept;
	}
	
	private Matcher<? super ConceptName> hasName(final OclConcept.Name name) {
		return new TypeSafeMatcher<ConceptName>(
		                                        ConceptName.class) {
			
			@Override
			public void describeTo(org.hamcrest.Description description) {
			}
			
			@Override
			public boolean matchesSafely(ConceptName actual) {
				Name actualName = new Name();
				actualName.copyFrom(actual);
				
				return actualName.equals(name);
			}
		};
	}
	
	private Matcher<Iterable<? extends ConceptName>> containsNamesInAnyOrder(List<OclConcept.Name> names) {
		List<Matcher<? super ConceptName>> matchers = new ArrayList<Matcher<? super ConceptName>>();
		for (Name name : names) {
			matchers.add(hasName(name));
		}
		
		return new IsIterableContainingInAnyOrder<ConceptName>(matchers);
	}
	
	private Matcher<Iterable<? extends ConceptDescription>> containsDescriptionsInAnyOrder(List<Description> descriptons) {
		List<Matcher<? super ConceptDescription>> matchers = new ArrayList<Matcher<? super ConceptDescription>>();
		for (Description description : descriptons) {
			matchers.add(hasDescription(description));
		}
		
		return new IsIterableContainingInAnyOrder<ConceptDescription>(matchers);
	}
	
	private Matcher<? super ConceptDescription> hasDescription(final Description description) {
		return new TypeSafeMatcher<ConceptDescription>(
		                                               ConceptDescription.class) {
			
			@Override
			public void describeTo(org.hamcrest.Description description) {
			}
			
			@Override
			protected boolean matchesSafely(ConceptDescription item) {
				Description actualDescription = new Description();
				actualDescription.copyFrom(item);
				return actualDescription.equals(description);
			}
		};
	}

	private interface IPredicate<T> { boolean apply(T type); }

	private static class CustomPredicate {
		public static Object predicateParams;

		public static <T> Collection<T> filter(Collection<T> target, IPredicate<T> predicate) {
			Collection<T> result = new ArrayList<T>();
			for (T element : target) {
				if (predicate.apply(element)) {
					result.add(element);
				}
			}
			return result;
		}

		public static <T> T select(Collection<T> target, IPredicate<T> predicate) {
			T result = null;
			for (T element : target) {
				if (!predicate.apply(element))
					continue;
				result = element;
				break;
			}
			return result;
		}

	}
}
