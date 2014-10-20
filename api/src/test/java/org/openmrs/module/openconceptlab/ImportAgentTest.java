package org.openmrs.module.openconceptlab;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

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
import org.openmrs.module.openconceptlab.OclConcept.Description;
import org.openmrs.module.openconceptlab.OclConcept.Name;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class ImportAgentTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	ImportAgent importAgent;
	
	@Autowired
	@Qualifier("conceptService")
	ConceptService conceptService;
	
	/**
	 * @see ImportAgent#importConcept(OclConcept,ImportQueue)
	 * @verifies save new concept
	 */
	@Test
	public void importConcept_shouldSaveNewConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		importConcept(oclConcept);
		assertImported(oclConcept);
	}
	
	/**
	 * @see ImportAgent#importConcept(OclConcept,ImportQueue)
	 * @verifies add new names to concept
	 */
	@Test
	public void importConcept_shouldAddNewNamesToConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		importConcept(oclConcept);
		
		Name thirdName = new Name();
		thirdName.setName("Third name");
		thirdName.setLocale(new Locale("pl", "PL"));
		thirdName.setLocalePreferred(true);
		thirdName.setNameType(ConceptNameType.FULLY_SPECIFIED.toString());
		oclConcept.getNames().add(thirdName);
		
		Name fourthName = newFourthName();
		oclConcept.getNames().add(fourthName);
		
		importConcept(oclConcept);
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
	 * @see ImportAgent#importConcept(OclConcept,ImportQueue)
	 * @verifies update name type in concept
	 */
	@Test
	public void importConcept_shouldUpdateNameTypeInConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		importConcept(oclConcept);
		
		for (Name name : oclConcept.getNames()) {
	        if (name.getNameType() == null) {
	        	name.setNameType(ConceptNameType.SHORT.toString());
	        }
        }
		
		importConcept(oclConcept);
		assertImported(oclConcept);		
	}
	
	/**
	 * @see ImportAgent#importConcept(OclConcept,ImportQueue)
	 * @verifies void names from concept
	 */
	@Test
	public void importConcept_shouldVoidNamesFromConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		Name fourthName = newFourthName();
		oclConcept.getNames().add(fourthName);
		importConcept(oclConcept);
		
		List<Name> voided = new ArrayList<OclConcept.Name>();;
		for (Iterator<Name> it = oclConcept.getNames().iterator(); it.hasNext();) {
	        Name name = it.next();
	        if (!name.isLocalePreferred()) {
	        	it.remove();
	        	voided.add(name);
	        }
        }
		assertThat(voided, is(not(empty())));
		
		importConcept(oclConcept);
		Concept concept = assertImported(oclConcept);
		
		Collection<ConceptName> nonVoidedNames = concept.getNames(false);
		Collection<ConceptName> voidedNames = new ArrayList<ConceptName>(concept.getNames(true));
		voidedNames.removeAll(nonVoidedNames);
		assertThat(voidedNames, containsNamesInAnyOrder(voided));
	}
	
	/**
	 * @see ImportAgent#importConcept(OclConcept,ImportQueue)
	 * @verifies add new descriptions to concept
	 */
	@Test
	public void importConcept_shouldAddNewDescriptionsToConcept() throws Exception {
	}
	
	/**
	 * @see ImportAgent#importConcept(OclConcept,ImportQueue)
	 * @verifies void descriptions from concept
	 */
	@Test
	public void importConcept_shouldVoidDescriptionsFromConcept() throws Exception {
	}
	
	/**
	 * @see ImportAgent#importConcept(OclConcept,ImportQueue)
	 * @verifies retire concept
	 */
	@Test
	public void importConcept_shouldRetireConcept() throws Exception {
	}
	
	/**
	 * @see ImportAgent#importConcept(OclConcept,ImportQueue)
	 * @verifies unretire concept
	 */
	@Test
	public void importConcept_shouldUnretireConcept() throws Exception {
	}
	
	/**
	 * @see ImportAgent#importConcept(OclConcept,ImportQueue)
	 * @verifies update datatype
	 */
	@Test
	public void importConcept_shouldUpdateDatatype() throws Exception {
	}
	
	/**
	 * @see ImportAgent#importConcept(OclConcept,ImportQueue)
	 * @verifies update concept class
	 */
	@Test
	public void importConcept_shouldUpdateConceptClass() throws Exception {
	}
	
	/**
	 * @see ImportAgent#importConcept(OclConcept,ImportQueue)
	 * @verifies fail if concept class missing
	 */
	@Test
	public void importConcept_shouldFailIfConceptClassMissing() throws Exception {
	}
	
	/**
	 * @see ImportAgent#importConcept(OclConcept,ImportQueue)
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
	
	public void importConcept(OclConcept oclConcept) {
		ImportQueue importQueue = new ImportQueue();
		importQueue.offer(oclConcept);
		
		while (!importQueue.isEmpty()) {
			importAgent.importConcept(null, importQueue);
		}
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
}
