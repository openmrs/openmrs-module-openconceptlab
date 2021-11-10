/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.importer;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSet;
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.CacheService;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.ValidationType;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclConcept.Description;
import org.openmrs.module.openconceptlab.client.OclConcept.Name;
import org.openmrs.module.openconceptlab.client.OclMapping;
import org.openmrs.module.openconceptlab.client.OclMapping.MapType;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;


public class SaverTest extends BaseModuleContextSensitiveTest {

	@Autowired
	Saver saver;

	@Autowired
	@Qualifier("conceptService")
	ConceptService conceptService;

	@Autowired @Qualifier("openconceptlab.importService")
	ImportService importService;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	private final SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss'Z'");
	
	private Import anImport;

	@Before
	public void startUpdate() {
		anImport = new Import();
		importService.saveSubscription(generateSubscription());
		importService.startImport(anImport);
	}

	@After
	public void stopUpdate() {
		importService.stopImport(importService.getLastImport());
	}

	private Subscription generateSubscription() {
		Subscription sub = new Subscription();
		sub.setToken("token");
		sub.setUrl("http://url.com");
		sub.setValidationType(ValidationType.FULL);
		return sub;
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies save new concept
	 */
	@Test
	public void importConcept_shouldSaveNewConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);
		assertImported(oclConcept);
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept) 
	 * @verifies add new names to concept
	 */
	@Test
	public void importConcept_shouldAddNewNamesToConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		Name thirdName = new Name();
		thirdName.setExternalId("9040fc62-fc52-4b54-a10b-3dfcdfa588e3");
		thirdName.setName("Third name");
		thirdName.setLocale(new Locale("pl", "PL"));
		thirdName.setLocalePreferred(true);
		thirdName.setNameType(ConceptNameType.FULLY_SPECIFIED.toString());
		oclConcept.getNames().add(thirdName);

		Name fourthName = newFourthName();
		oclConcept.getNames().add(fourthName);

		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);
		assertImported(oclConcept);
	}

	private Name newFourthName() {
		Name fourthName = new Name();
		fourthName.setExternalId("a9105ff6-8f9c-449a-9d71-e8b819cc2452");
		fourthName.setName("Fourth name");
		fourthName.setLocale(Context.getLocale());
		fourthName.setLocalePreferred(false);
		fourthName.setNameType(ConceptNameType.INDEX_TERM.toString());
		return fourthName;
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies anImport name type in concept
	 */
	@Test
	public void importConcept_shouldUpdateNameTypeInConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		for (Name name : oclConcept.getNames()) {
			if (name.getNameType() == null) {
				name.setNameType(ConceptNameType.INDEX_TERM.toString());
			}
		}

		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);
		assertImported(oclConcept);
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies anImport name type in same name with different uuid
	 */
	@Test
	public void importConcept_shouldUpdateNamesWithDifferentUuids() throws Exception {
		OclConcept oclConcept = newOclConcept();
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		for (Name name : oclConcept.getNames()) {
			name.setExternalId(UUID.randomUUID().toString());
		}

		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);
		assertImported(oclConcept);

		Concept concept = conceptService.getConceptByUuid(oclConcept.getExternalId());

		List<Matcher<? super ConceptName>> matchers = new ArrayList<Matcher<? super ConceptName>>();
		for (Name name : oclConcept.getNames()) {
			ConceptNameType nameType = (name.getNameType() != null) ? ConceptNameType.valueOf(name.getNameType()) : null;
	        matchers.add(allOf(hasProperty("name", is(name.getName())), hasProperty("conceptNameType", is(nameType))));
        }

		assertThat(concept.getNames(false), containsInAnyOrder(matchers));
		assertThat(concept.getNames(true).size(), is(6));
		//3 names must have been voided
		assertThat(concept.getNames(false).size(), is(3));
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies anImport datatype
	 */
	@Test
	public void importConcept_shouldUpdateDatatype() throws Exception {
		OclConcept oclConcept = newOclConcept();
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		String initialDatatype = oclConcept.getDatatype();

		oclConcept.setDatatype("Numeric");
		String updatedDatatype = oclConcept.getDatatype();

		assertFalse(initialDatatype.equals(updatedDatatype));

		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);
		assertImported(oclConcept);
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 */
	@Test
	public void importConcept_shouldAcceptNoneDatatype() throws Exception {
		OclConcept oclConcept = newOclConcept();
		oclConcept.setDatatype("None");
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		assertThat(oclConcept.getDatatype(), equalTo("N/A"));

		assertImported(oclConcept);
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 */
	@Test
	public void importConcept_shouldAcceptFullySpecifiedNameType() throws Exception {
		OclConcept oclConcept = newOclConcept();
		oclConcept.getNames().get(0).setNameType("Fully Specified");
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		assertThat(oclConcept.getNames().get(0).getNameType(), equalTo("FULLY_SPECIFIED"));

		assertImported(oclConcept);
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies anImport concept class
	 */
	@Test
	public void importConcept_shouldUpdateConceptClass() throws Exception {
		OclConcept oclConcept = newOclConcept();
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		String initialConceptClass = oclConcept.getConceptClass();

		oclConcept.setConceptClass("Procedure");
		String updatedConceptClass = oclConcept.getConceptClass();

		assertFalse(initialConceptClass.equals(updatedConceptClass));

		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);
		assertImported(oclConcept);
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies void names from concept
	 */
	@Test
	public void importConcept_shouldVoidNamesFromConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		Name fourthName = newFourthName();
		oclConcept.getNames().add(fourthName);
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		List<Name> voided = new ArrayList<OclConcept.Name>();
		for (Iterator<Name> it = oclConcept.getNames().iterator(); it.hasNext();) {
			Name name = it.next();
			if (!name.isLocalePreferred()) {
				it.remove();
				voided.add(name);
			}
		}
		assertThat(voided, is(not(empty())));

		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);
		Concept concept = assertImported(oclConcept);

		Collection<ConceptName> nonVoidedNames = concept.getNames(false);
		Collection<ConceptName> voidedNames = new ArrayList<ConceptName>(concept.getNames(true));
		voidedNames.removeAll(nonVoidedNames);
		assertThat(voidedNames, containsNamesInAnyOrder(voided));
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies add new descriptions to concept
	 */
	@Test
	public void importConcept_shouldAddNewDescriptionsToConcept() throws Exception {

		OclConcept oclConcept = newOclConcept();
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		Description desc1 = new Description();
		desc1.setDescription("test oclConceptDescription");
		desc1.setLocale(Context.getLocale());
		oclConcept.getDescriptions().add(desc1);

		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		assertImported(oclConcept);

	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies void descriptions from concept
	 */
	@Test
	public void importConcept_shouldVoidDescriptionsFromConcept() throws Exception {

		OclConcept oclConcept = newOclConcept();
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		Description desc1 = new Description();
		desc1.setExternalId("7cc35481-ce72-4615-b857-a944b25e9c43");
		desc1.setDescription("test oclConceptDescription");
		desc1.setLocale(Context.getLocale());
		oclConcept.getDescriptions().add(desc1);

		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);
		Concept concept = assertImported(oclConcept);

		//cloning object to save state of descriptions after importing again
		Concept cloned = (Concept) org.apache.commons.lang.SerializationUtils.clone(concept);

		Collection<ConceptDescription> descriptionsBeforeVoiding = cloned.getDescriptions();

		List<Description> voided = new ArrayList<OclConcept.Description>();
		for (Iterator<Description> it = oclConcept.getDescriptions().iterator(); it.hasNext();) {
			Description description = it.next();
			if (description.getDescription().equals(desc1.getDescription())) {
				it.remove();
				voided.add(description);
			}
		}
		assertThat(voided, is(not(empty())));

		//at this point without cloning object original desc collecion is lost
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);
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
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies retire concept
	 */
	@Test
	public void importConcept_shouldRetireConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		assertFalse(oclConcept.isRetired());
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		oclConcept.setRetired(true);

		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		Concept concept = assertImported(oclConcept);
		assertTrue(concept.isRetired());
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies unretire concept
	 */
	@Test
	public void importConcept_shouldUnretireConcept() throws Exception {

		OclConcept oclConcept = newOclConcept();
		oclConcept.setRetired(true);
		assertTrue(oclConcept.isRetired());
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		oclConcept.setRetired(false);

		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		Concept concept = assertImported(oclConcept);
		assertFalse(concept.isRetired());

	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies create concept class missing if missing
	 */
	@Test
	public void importConcept_shouldCreateConceptClassIfMissing() throws Exception {
		Import update = importService.getLastImport();

		OclConcept concept = newOclConcept();
		concept.setConceptClass("Some missing concept class");

		saver.saveConcept(new CacheService(conceptService), update, concept);

		ConceptClass conceptClass = conceptService.getConceptClassByName(concept.getConceptClass());
		assertThat(conceptClass, notNullValue());
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies fail if concept class missing
	 */
	@Test
	public void importConcept_shouldFailIfConceptClassMissing() throws Exception {
		OclConcept oclConcept = newOclConcept();
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);

		oclConcept.setConceptClass(null);
		exception.expect(ImportException.class);
		saver.saveConcept(new CacheService(conceptService), anImport, oclConcept);
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies fail if datatype missing
	 */
	@Test
	public void importConcept_shouldFailIfDatatypeMissing() throws Exception {
		Import update = importService.getLastImport();

		OclConcept concept = newOclConcept();
		concept.setDatatype("Some missing datatype");

		exception.expect(ImportException.class);
		exception.expectMessage("Cannot create concept /orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		saver.saveConcept(new CacheService(conceptService), update, concept);
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies change duplicate synonym to index term
	 */
	@Test
	public void importConcept_shouldChangeDuplicateSynonymToIndexTerm() throws Exception {
		Import update = importService.getLastImport();

		OclConcept concept = newOclConcept();

		Name polishName = new Name();
		polishName.setExternalId(UUID.randomUUID().toString());
		polishName.setName("Nazwa");
		polishName.setLocale(new Locale("pl"));
		polishName.setNameType("FULLY_SPECIFIED");
		polishName.setLocalePreferred(true);
		concept.getNames().add(polishName);

		saver.saveConcept(new CacheService(conceptService), update, concept);

		OclConcept conceptWithSynonym = newOtherOclConcept();
		Name otherPolishName = new Name();
		otherPolishName.setExternalId(UUID.randomUUID().toString());
		otherPolishName.setName("Nazwa");
		otherPolishName.setLocale(new Locale("pl"));
		otherPolishName.setLocalePreferred(true);
		conceptWithSynonym.getNames().add(otherPolishName);

		saver.saveConcept(new CacheService(conceptService), update, conceptWithSynonym);

		Concept importedConcept = conceptService.getConceptByUuid(concept.getExternalId());
		Concept importedConceptWithIndexTerm = conceptService.getConceptByUuid(conceptWithSynonym.getExternalId());

		assertThat(importedConcept.getNames(), hasItem((Matcher<? super ConceptName>) allOf(hasProperty("conceptNameType", equalTo(ConceptNameType.FULLY_SPECIFIED)),
			hasProperty("name", equalTo("Nazwa")))));

		assertThat(importedConceptWithIndexTerm.getNames(), hasItem((Matcher<? super ConceptName>) allOf(hasProperty("conceptNameType", equalTo(ConceptNameType.INDEX_TERM)),
			hasProperty("name", equalTo("Nazwa")))));
	}
	
	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies save concept if versionUrl changed from last anImport
	 */
	@Test
	public void importConcept_shouldUpdateConceptIfVersionUrlChanged() throws Exception {
		Import update = importService.getLastImport();
		OclConcept concept = newOclConcept();
		importService.saveItem(saver.saveConcept(new CacheService(conceptService), update, concept));
		
		OclConcept updateConcept = newOclConcept();
		updateConcept.setVersionUrl(newOtherOclConcept().getVersionUrl());
		updateConcept.setDatatype("Document");
		
		Item item = saver.saveConcept(new CacheService(conceptService), update, updateConcept);
		assertThat(item, hasProperty("state", equalTo(ItemState.UPDATED)));
		
		Concept importedConcept = conceptService.getConceptByUuid(concept.getExternalId());
		assertThat(importedConcept.getDatatype(), hasProperty("name", equalTo(updateConcept.getDatatype())));
	}
	
	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies skip saving concept if versionUrl didn't change from last anImport
	 */
	@Test
	public void importConcept_shouldSkipUpdatingConceptIfVersionUrlDidntChange() throws Exception {
		Import update = importService.getLastImport();
		OclConcept concept = newOclConcept();
		OclConcept updateConcept = newOclConcept();
			
		importService.saveItem(saver.saveConcept(new CacheService(conceptService), update, concept));
		
		updateConcept.setDatatype("Document");		
		Item item = saver.saveConcept(new CacheService(conceptService), update, updateConcept);
		assertThat(item, hasProperty("state", equalTo(ItemState.UP_TO_DATE)));
		
		Concept importedConcept = conceptService.getConceptByUuid(concept.getExternalId());
		assertThat(importedConcept.getDatatype(), hasProperty("name", equalTo(concept.getDatatype())));
	}

	/**
	 * @see Saver#saveConcept(CacheService, Import, OclConcept)
	 * @verifies change duplicate fully specified name to index term
	 */
	@Test
	public void importConcept_shouldChangeDuplicateFullySpecifiedNamesToIndexTerm() throws Exception {
		Import update = importService.getLastImport();

		OclConcept concept = newOclConcept();

		Name polishName = new Name();
		polishName.setExternalId(UUID.randomUUID().toString());
		polishName.setName("Nazwa");
		polishName.setLocale(new Locale("pl"));
		polishName.setNameType("FULLY_SPECIFIED");
		polishName.setLocalePreferred(true);
		concept.getNames().add(polishName);

		saver.saveConcept(new CacheService(conceptService), update, concept);

		OclConcept conceptWithSynonym = newOtherOclConcept();
		Name otherPolishName = new Name();
		otherPolishName.setExternalId(UUID.randomUUID().toString());
		otherPolishName.setName("Nazwa");
		otherPolishName.setLocale(new Locale("pl"));
		otherPolishName.setNameType("FULLY_SPECIFIED");
		otherPolishName.setLocalePreferred(true);
		conceptWithSynonym.getNames().add(otherPolishName);

		saver.saveConcept(new CacheService(conceptService), update, conceptWithSynonym);

		Concept importedConcept = conceptService.getConceptByUuid(concept.getExternalId());
		Concept importedConceptWithIndexTerm = conceptService.getConceptByUuid(conceptWithSynonym.getExternalId());

		assertThat(importedConcept.getNames(), hasItem((Matcher<? super ConceptName>) allOf(hasProperty("conceptNameType", equalTo(ConceptNameType.FULLY_SPECIFIED)),
			hasProperty("name", equalTo("Nazwa")))));

		assertThat(importedConceptWithIndexTerm.getNames(), hasItem((Matcher<? super ConceptName>) allOf(hasProperty("conceptNameType", equalTo(ConceptNameType.INDEX_TERM)),
			hasProperty("name", equalTo("Nazwa")))));
	}

	@Test
	public void importMapping_shouldAddConceptAnswer() throws Exception {
		Import update = importService.getLastImport();

		OclConcept question = newOclConcept();
		importService.saveItem(saver.saveConcept(new CacheService(conceptService), update, question));

		OclConcept answer = newOtherOclConcept();
		importService.saveItem(saver.saveConcept(new CacheService(conceptService), update, answer));

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");

		oclMapping.setMapType(MapType.Q_AND_A);
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/");

		saver.saveMapping(new CacheService(conceptService), update, oclMapping);

		Concept questionConcept = conceptService.getConceptByUuid(question.getExternalId());
		Concept answerConcept = conceptService.getConceptByUuid(answer.getExternalId());

		assertThat(questionConcept.getAnswers(), contains(hasQuestionAndAnswer(questionConcept, answerConcept)));
	}

	@Test
	public void importMapping_shouldAddConceptAnswerWithSortWeight() throws Exception {
		Import update = importService.getLastImport();

		OclConcept question = newOclConcept();
		importService.saveItem(saver.saveConcept(new CacheService(conceptService), update, question));

		OclConcept answer = newOtherOclConcept();
		importService.saveItem(saver.saveConcept(new CacheService(conceptService), update, answer));

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		oclMapping.setMapType(MapType.Q_AND_A);
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/");
		oclMapping.setExtras(newOclMappingExtras(356.0));

		saver.saveMapping(new CacheService(conceptService), update, oclMapping);

		Concept questionConcept = conceptService.getConceptByUuid(question.getExternalId());
		Concept answerConcept = conceptService.getConceptByUuid(answer.getExternalId());
		List<ConceptAnswer> answers = answersForConcept(questionConcept, answerConcept);
		assertThat(answers.size(), is(1));
		assertThat(answers.get(0).getSortWeight(), is(356.0));
	}

	@Test
	public void importMapping_shouldRemoveConceptAnswer() throws Exception {
		importMapping_shouldAddConceptAnswer();

		Import update = importService.getLastImport();

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");

		oclMapping.setMapType(MapType.Q_AND_A);
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/");
		oclMapping.setRetired(true);

		saver.saveMapping(new CacheService(conceptService), update, oclMapping);

		Concept questionConcept = conceptService.getConceptByUuid("6c1bbb30-55f6-11e4-8ed6-0800200c9a66");

		assertThat(questionConcept.getAnswers(), is(empty()));
	}

	@Test
	public void importMapping_shouldUpdateConceptAnswer() throws Exception {
		importMapping_shouldAddConceptAnswer();
		Import update = importService.getLastImport();

		Concept questionConcept = conceptService.getConceptByUuid(newOclConcept().getExternalId());
		Concept answerConcept = conceptService.getConceptByUuid(newOtherOclConcept().getExternalId());

		List<ConceptAnswer> answers = answersForConcept(questionConcept, answerConcept);
		assertThat(answers.size(), is(1));
		assertThat(answers.get(0).getSortWeight(), is(1.0)); // Concept Answers are given a sort weight in core if null

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		oclMapping.setMapType(MapType.Q_AND_A);
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/");
		oclMapping.setExtras(newOclMappingExtras(15.0));

		saver.saveMapping(new CacheService(conceptService), update, oclMapping);
		answers = answersForConcept(questionConcept, answerConcept);
		assertThat(answers.size(), is(1));
		assertThat(answers.get(0).getSortWeight(), is(15.0));
	}

	@Test
	public void importMapping_shouldAddConceptSetMember() throws Exception {
		Import update = importService.getLastImport();

		OclConcept set = newOclConcept();
		importService.saveItem(saver.saveConcept(new CacheService(conceptService), update, set));

		OclConcept member = newOtherOclConcept();
		importService.saveItem(saver.saveConcept(new CacheService(conceptService), update, member));

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");

		oclMapping.setMapType(MapType.SET);
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/");

		saver.saveMapping(new CacheService(conceptService), update, oclMapping);

		Concept setConcept = conceptService.getConceptByUuid(set.getExternalId());
		Concept memberConcept = conceptService.getConceptByUuid(member.getExternalId());

		assertThat(setConcept.getSetMembers(), contains(memberConcept));
	}

	@Test
	public void importMapping_shouldAddConceptSetMemberWithSortWeight() throws Exception {
		Import update = importService.getLastImport();

		OclConcept set = newOclConcept();
		importService.saveItem(saver.saveConcept(new CacheService(conceptService), update, set));

		OclConcept member = newOtherOclConcept();
		importService.saveItem(saver.saveConcept(new CacheService(conceptService), update, member));

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");

		oclMapping.setMapType(MapType.SET);
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/");
		oclMapping.setExtras(newOclMappingExtras(11.0));

		saver.saveMapping(new CacheService(conceptService), update, oclMapping);

		Concept setConcept = conceptService.getConceptByUuid(set.getExternalId());
		Concept memberConcept = conceptService.getConceptByUuid(member.getExternalId());
		List<ConceptSet> members = membersForConceptSet(setConcept, memberConcept);
		assertThat(members.size(), is(1));
		assertThat(members.get(0).getSortWeight(), is(11.0));
	}

	@Test
	public void importMapping_shouldRemoveConceptSetMember() throws Exception {
		importMapping_shouldAddConceptSetMember();

		Import update = importService.getLastImport();

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");

		oclMapping.setMapType(MapType.SET);
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/");
		oclMapping.setRetired(true);

		saver.saveMapping(new CacheService(conceptService), update, oclMapping);

		Concept setConcept = conceptService.getConceptByUuid("6c1bbb30-55f6-11e4-8ed6-0800200c9a66");

		assertThat(setConcept.getSetMembers(), is(empty()));
	}

	@Test
	public void importMapping_shouldUpdateConceptSetMember() throws Exception {

		importMapping_shouldAddConceptSetMember();
		Import update = importService.getLastImport();

		Concept setConcept = conceptService.getConceptByUuid(newOclConcept().getExternalId());
		Concept memberConcept = conceptService.getConceptByUuid(newOtherOclConcept().getExternalId());

		List<ConceptSet> members = membersForConceptSet(setConcept, memberConcept);
		assertThat(members.size(), is(1));
		assertThat(members.get(0).getSortWeight(), is(1.0));

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		oclMapping.setMapType(MapType.SET);
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/");
		oclMapping.setExtras(new OclMapping.Extras());
		oclMapping.getExtras().setSortWeight(5.0);

		saver.saveMapping(new CacheService(conceptService), update, oclMapping);

		members = membersForConceptSet(setConcept, memberConcept);
		assertThat(members.size(), is(1));
		assertThat(members.get(0).getSortWeight(), is(5.0));
	}

	@Test
	@Ignore
	public void importMapping_shouldAddConceptMappingAndTerm() throws Exception {
		Import update = importService.getLastImport();

		OclConcept oclConcept = newOclConcept();
		importService.saveItem(saver.saveConcept(new CacheService(conceptService), update, oclConcept));

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");

		oclMapping.setMapType("SAME-AS");
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToSourceName("SNOMED CT");
		oclMapping.setToConceptCode("1001");

		saver.saveMapping(new CacheService(conceptService), update, oclMapping);

		Concept concept = conceptService.getConceptByUuid("6c1bbb30-55f6-11e4-8ed6-0800200c9a66");

		ConceptSource source = conceptService.getConceptSourceByName("SNOMED CT");
		ConceptMapType mapType = conceptService.getConceptMapTypeByName("SAME_AS");
		assertThat(concept.getConceptMappings(), contains(hasMapping(source, "1001", mapType)));
	}

	@Test
	public void importMapping_shouldRemoveConceptMappingAndRetireTerm() throws Exception {
		importMapping_shouldAddConceptMappingAndTerm();

		Import update = importService.getLastImport();

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");

		oclMapping.setMapType("SAME-AS");
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToSourceName("SNOMED CT");
		oclMapping.setToConceptCode("1001");
		oclMapping.setRetired(true);

		saver.saveMapping(new CacheService(conceptService), update, oclMapping);

		Concept concept = conceptService.getConceptByUuid("6c1bbb30-55f6-11e4-8ed6-0800200c9a66");
		ConceptSource source = conceptService.getConceptSourceByName("SNOMED CT");
		ConceptReferenceTerm term = conceptService.getConceptReferenceTermByCode("1001", source);

		assertThat(concept.getConceptMappings(), is(empty()));
		assertThat(term.isRetired(), is(true));
	}

	@Test
	public void importMapping_addConceptMappingAndUnretireTerm() throws Exception {
		importMapping_shouldAddConceptMappingAndTerm();
		importMapping_shouldRemoveConceptMappingAndRetireTerm();

		Import update = importService.getLastImport();

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");

		oclMapping.setMapType("SAME-AS");
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToSourceName("SNOMED CT");
		oclMapping.setToConceptCode("1001");
		oclMapping.setRetired(false);

		saver.saveMapping(new CacheService(conceptService), update, oclMapping);

		Concept concept = conceptService.getConceptByUuid("6c1bbb30-55f6-11e4-8ed6-0800200c9a66");

		ConceptSource source = conceptService.getConceptSourceByName("SNOMED CT");
		ConceptMapType mapType = conceptService.getConceptMapTypeByName("SAME_AS");
		assertThat(concept.getConceptMappings(), contains(hasMapping(source, "1001", mapType)));
	}
	
	@Test
	public void importMapping_shouldUpdateMappingOnylIfItHasBeenUpdatedSinceLastImport() throws Exception {
		OclConcept oclConcept = newOclConcept();
		importService.saveItem(saver.saveConcept(new CacheService(conceptService), anImport, oclConcept));
		Import update = importService.getLastImport();

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		oclMapping.setMapType("SAME-AS");
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToSourceName("SNOMED CT");
		oclMapping.setToConceptCode("1001");
		oclMapping.setUrl("/orgs/CIELTEST/sources/CIELTEST/mappings/303");

		importService.saveItem(saver.saveMapping(new CacheService(conceptService), update, oclMapping));
		
		oclMapping.setUpdatedOn(dateFormat.parse("2008-02-18T09:10:16Z"));
		
		Item item = saver.saveMapping(new CacheService(conceptService), update, oclMapping);
		assertThat(item, hasProperty("state", equalTo(ItemState.UPDATED)));
		importService.saveItem(item);
		
		item = saver.saveMapping(new CacheService(conceptService), update, oclMapping);
		assertThat(item, hasProperty("state", equalTo(ItemState.UP_TO_DATE)));
	}
	
	@Test
	public void isMappingUpToDate_shouldReturnIfMappingUpdateOnIsAfter() throws Exception{
		Import update = importService.getLastImport();

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		oclMapping.setMapType("SAME-AS");
		oclMapping.setUpdatedOn(dateFormat.parse("2008-02-18T09:10:16Z"));
		
		Item item = new Item(update, oclMapping, ItemState.ADDED);
		
		oclMapping.setUpdatedOn(dateFormat.parse("2008-02-18T09:10:16Z"));
		assertTrue(saver.isMappingUpToDate(item, oclMapping));
	}
	
	@Test
	public void isMappingUpToDate_shouldReturnTrueIfItemUpdateOnIsNull() throws Exception{
		Import update = importService.getLastImport();

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		oclMapping.setMapType("SAME-AS");
		
		Item item = new Item(update, oclMapping, ItemState.ADDED);
		
		oclMapping.setUpdatedOn(dateFormat.parse("2010-02-18T09:10:16Z"));
		
		assertFalse(saver.isMappingUpToDate(item, oclMapping));
	}
	@Test
	public void isMappingUpToDate_shouldReturnTrueIfBothUpdatedOnAreNull() throws Exception{
		Import update = importService.getLastImport();

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		oclMapping.setMapType("SAME-AS");
		
		Item item = new Item(update, oclMapping, ItemState.ADDED);
		
		assertTrue(saver.isMappingUpToDate(item, oclMapping));
	}

	@Test
	public void saveMapping_ShouldNotSaveReferenceConceptIfMappingNotQnAorSet() {

		Import update = importService.getLastImport();

		OclConcept oclConcept = newOclConcept();

		importService.saveItem(saver.saveConcept(new CacheService(conceptService), update, oclConcept));

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");

		oclMapping.setMapType("SAME-AS");
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/100002/");
		oclMapping.setToSourceName("CIELTEST");
		oclMapping.setToConceptCode("100002");

		saver.saveMapping(new CacheService(conceptService), update, oclMapping);

		Concept toConcept = conceptService.getConcept(100002);
		assertEquals(null, toConcept);

	}

	@Test
	public void saveMapping_ShouldSaveMappingIfNotQnAorSetAndReferencedConceptDoesNotExist() {

		Import update = importService.getLastImport();

		OclConcept oclConcept = newOclConcept();

		importService.saveItem(saver.saveConcept(new CacheService(conceptService), update, oclConcept));

		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");

		oclMapping.setMapType("SAME-AS");
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/100002/");
		oclMapping.setToSourceName("CIELTEST");
		oclMapping.setToConceptCode("100002");

		saver.saveMapping(new CacheService(conceptService), update, oclMapping);

		Concept fromConcept = conceptService.getConceptByUuid(oclConcept.getExternalId());
		assertThat(fromConcept.getConceptMappings().size(), is(1));

	}

	public OclConcept newOclConcept() {
		OclConcept oclConcept = new OclConcept();

		oclConcept.setExternalId("6c1bbb30-55f6-11e4-8ed6-0800200c9a66");

		oclConcept.setConceptClass("Test");
		oclConcept.setDatatype("N/A");
		oclConcept.setDateCreated(new Date());
		oclConcept.setDateUpdated(new Date());

		oclConcept.setUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclConcept.setVersionUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/54ea96d28a86f20421474a3a/");

		List<Description> descriptons = new ArrayList<OclConcept.Description>();
		Description description = new Description();
		description.setExternalId("a54594cf-7612-46c3-90f3-10599f4e3223");
		description.setDescription("Test description");
		description.setLocale(Context.getLocale());
		descriptons.add(description);
		oclConcept.setDescriptions(descriptons);

		List<Name> names = new ArrayList<OclConcept.Name>();
		Name name = new Name();
		name.setExternalId("051ba9d7-755a-4301-87b3-8e6466f3d3fd");
		name.setName("Test name");
		name.setLocale(Context.getLocale());
		name.setLocalePreferred(true);
		name.setNameType(ConceptNameType.FULLY_SPECIFIED.toString());
		names.add(name);

		Name secondName = new Name();
		secondName.setExternalId("e24eef27-60fa-41d2-ae23-93cc1e6bb153");
		secondName.setName("Second name");
		secondName.setLocale(Context.getLocale());
		secondName.setLocalePreferred(false);
		names.add(secondName);

		Name secondNameShort = new Name();
		secondNameShort.setExternalId("ebfd0b33-44e5-43d1-b2d9-9a08f9f3c230");
		secondNameShort.setName("Second name");
		secondNameShort.setLocale(Context.getLocale());
		secondNameShort.setLocalePreferred(false);
		secondNameShort.setNameType(ConceptNameType.SHORT.toString());
		names.add(secondNameShort);

		oclConcept.setNames(names);

		return oclConcept;
	}

	public OclConcept newOtherOclConcept() {
		OclConcept oclConcept = new OclConcept();

		oclConcept.setExternalId("83f7d006-054a-4a76-a7ef-dc5cfbd125d2");

		oclConcept.setConceptClass("Test");
		oclConcept.setDatatype("N/A");
		oclConcept.setDateCreated(new Date());
		oclConcept.setDateUpdated(new Date());

		oclConcept.setUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/");
		oclConcept.setVersionUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/54ea96d38a86f20421474a3c/");

		List<Description> descriptons = new ArrayList<OclConcept.Description>();
		Description description = new Description();
		description.setExternalId("f6b743ac-1210-4953-bc98-db4e805754b9");
		description.setDescription("Other description");
		description.setLocale(Context.getLocale());
		descriptons.add(description);
		oclConcept.setDescriptions(descriptons);

		List<Name> names = new ArrayList<OclConcept.Name>();
		Name name = new Name();
		name.setExternalId("9fc16f6f-d016-493a-95fd-26284853c064");
		name.setName("Other name");
		name.setLocale(Context.getLocale());
		name.setLocalePreferred(true);
		name.setNameType(ConceptNameType.FULLY_SPECIFIED.toString());
		names.add(name);

		Name secondName = new Name();
		secondName.setExternalId("54ff1113-3705-4510-b01b-1d89cc09b912");
		secondName.setName("Other second name");
		secondName.setLocale(Context.getLocale());
		secondName.setLocalePreferred(false);
		names.add(secondName);

		oclConcept.setNames(names);

		return oclConcept;
	}

	private List<ConceptAnswer> answersForConcept(Concept question, Concept answer) {
		List<ConceptAnswer> l = new ArrayList<>();
		for (ConceptAnswer ca : question.getAnswers()) {
			if (ca.getConcept().equals(question) && ca.getAnswerConcept().equals(answer)) {
				l.add(ca);
			}
		}
		return l;
	}

	private List<ConceptSet> membersForConceptSet(Concept set, Concept member) {
		List<ConceptSet> l = new ArrayList<>();
		for (ConceptSet cs : set.getConceptSets()) {
			if (cs.getConceptSet().equals(set) && cs.getConcept().equals(member)) {
				l.add(cs);
			}
		}
		return l;
	}

	private OclMapping.Extras newOclMappingExtras(Double sortWeight) {
		OclMapping.Extras extras = new OclMapping.Extras();
		extras.setSortWeight(sortWeight);
		return extras;
	}

	private Concept assertImported(OclConcept oclConcept) {
		Concept concept = conceptService.getConceptByUuid(oclConcept.getExternalId());
		assertThat(concept, is(notNullValue()));

		ConceptClass conceptClass = conceptService.getConceptClassByName(oclConcept.getConceptClass());
		assertThat(concept.getConceptClass(), is(conceptClass));

		ConceptDatatype conceptDatatype = conceptService.getConceptDatatypeByName(oclConcept.getDatatype());
		assertThat(concept.getDatatype(), is(conceptDatatype));
		assertThat(concept.getNames(false), containsNamesInAnyOrder(oclConcept.getNames()));
		assertThat(concept.getDescriptions(), containsDescriptionsInAnyOrder(oclConcept.getDescriptions()));

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

	private Matcher<? super ConceptAnswer> hasQuestionAndAnswer(final Concept question, final Concept answer) {
		return new TypeSafeMatcher<ConceptAnswer>(
		                                          ConceptAnswer.class) {

			@Override
			public void describeTo(org.hamcrest.Description description) {
			}

			@Override
			protected boolean matchesSafely(ConceptAnswer item) {
				return answer.equals(item.getAnswerConcept()) && question.equals(item.getConcept());
			}
		};
	}

	private Matcher<? super ConceptMap> hasMapping(final ConceptSource source, final String code,
	        final ConceptMapType mapType) {
		return new TypeSafeMatcher<ConceptMap>(
		                                       ConceptMap.class) {

			@Override
			public void describeTo(org.hamcrest.Description description) {
			}

			@Override
			protected boolean matchesSafely(ConceptMap item) {
				return new EqualsBuilder().append(item.getConceptMapType(), mapType)
				        .append(item.getConceptReferenceTerm().getConceptSource(), source)
				        .append(item.getConceptReferenceTerm().getCode(), code).build();
			}
		};
	}

	private interface IPredicate<T> {

		boolean apply(T type);
	}

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
