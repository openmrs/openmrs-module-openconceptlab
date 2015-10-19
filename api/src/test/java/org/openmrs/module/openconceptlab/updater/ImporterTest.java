/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.updater;

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
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.CacheService;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclConcept.Description;
import org.openmrs.module.openconceptlab.client.OclConcept.Name;
import org.openmrs.module.openconceptlab.client.OclMapping;
import org.openmrs.module.openconceptlab.client.OclMapping.MapType;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class ImporterTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	Importer importer;
	
	@Autowired
	@Qualifier("conceptService")
	ConceptService conceptService;
	
	@Autowired
	UpdateService updateService;
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	Update update;
	
	@Before
	public void startUpdate() {
		update = new Update();
		updateService.startUpdate(update);
	}
	
	@After
	public void stopUpdate() {
		updateService.stopUpdate(updateService.getLastUpdate());
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies save new concept
	 */
	@Test
	public void importConcept_shouldSaveNewConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		assertImported(oclConcept);
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies add new names to concept
	 */
	@Test
	public void importConcept_shouldAddNewNamesToConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		
		Name thirdName = new Name();
		thirdName.setExternalId("9040fc62-fc52-4b54-a10b-3dfcdfa588e3");
		thirdName.setName("Third name");
		thirdName.setLocale(new Locale("pl", "PL"));
		thirdName.setLocalePreferred(true);
		thirdName.setNameType(ConceptNameType.FULLY_SPECIFIED.toString());
		oclConcept.getNames().add(thirdName);
		
		Name fourthName = newFourthName();
		oclConcept.getNames().add(fourthName);
		
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
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
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies update name type in concept
	 */
	@Test
	public void importConcept_shouldUpdateNameTypeInConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		
		for (Name name : oclConcept.getNames()) {
			if (name.getNameType() == null) {
				name.setNameType(ConceptNameType.INDEX_TERM.toString());
			}
		}
		
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		assertImported(oclConcept);
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies update name type in same name with different uuid
	 */
	@Test
	public void importConcept_shouldUpdateNamesWithDifferentUuids() throws Exception {
		OclConcept oclConcept = newOclConcept();
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		
		for (Name name : oclConcept.getNames()) {
			name.setExternalId(UUID.randomUUID().toString());
		}
		
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
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
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies void names from concept
	 */
	@Test
	public void importConcept_shouldVoidNamesFromConcept() throws Exception {
		OclConcept oclConcept = newOclConcept();
		Name fourthName = newFourthName();
		oclConcept.getNames().add(fourthName);
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		
		List<Name> voided = new ArrayList<OclConcept.Name>();
		for (Iterator<Name> it = oclConcept.getNames().iterator(); it.hasNext();) {
			Name name = it.next();
			if (!name.isLocalePreferred()) {
				it.remove();
				voided.add(name);
			}
		}
		assertThat(voided, is(not(empty())));
		
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
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
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		
		Description desc1 = new Description();
		desc1.setDescription("test oclConceptDescription");
		desc1.setLocale(Context.getLocale());
		oclConcept.getDescriptons().add(desc1);
		
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		
		assertImported(oclConcept);
		
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies void descriptions from concept
	 */
	@Test
	public void importConcept_shouldVoidDescriptionsFromConcept() throws Exception {
		
		OclConcept oclConcept = newOclConcept();
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		
		Description desc1 = new Description();
		desc1.setExternalId("7cc35481-ce72-4615-b857-a944b25e9c43");
		desc1.setDescription("test oclConceptDescription");
		desc1.setLocale(Context.getLocale());
		oclConcept.getDescriptons().add(desc1);
		
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		Concept concept = assertImported(oclConcept);
		
		//cloning object to save state of descriptions after importing again
		Concept cloned = (Concept) org.apache.commons.lang.SerializationUtils.clone(concept);
		
		Collection<ConceptDescription> descriptionsBeforeVoiding = cloned.getDescriptions();
		
		List<Description> voided = new ArrayList<OclConcept.Description>();
		for (Iterator<Description> it = oclConcept.getDescriptons().iterator(); it.hasNext();) {
			Description description = it.next();
			if (description.getDescription().equals(desc1.getDescription())) {
				it.remove();
				voided.add(description);
			}
		}
		assertThat(voided, is(not(empty())));
		
		//at this point without cloning object original desc collecion is lost
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
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
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		
		oclConcept.setRetired(true);
		
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		
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
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		
		oclConcept.setRetired(false);
		
		importer.importConcept(new CacheService(conceptService), update, oclConcept);
		
		Concept concept = assertImported(oclConcept);
		assertFalse(concept.isRetired());
		
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies create concept class missing if missing
	 */
	@Test
	public void importConcept_shouldCreateConceptClassIfMissing() throws Exception {
		Update update = updateService.getLastUpdate();
		
		OclConcept concept = newOclConcept();
		concept.setConceptClass("Some missing concept class");
		
		importer.importConcept(new CacheService(conceptService), update, concept);
		
		ConceptClass conceptClass = conceptService.getConceptClassByName(concept.getConceptClass());
		assertThat(conceptClass, notNullValue());
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies fail if datatype missing
	 */
	@Test
	public void importConcept_shouldFailIfDatatypeMissing() throws Exception {
		Update update = updateService.getLastUpdate();
		
		OclConcept concept = newOclConcept();
		concept.setDatatype("Some missing datatype");
		
		exception.expect(ImportException.class);
		exception.expectMessage("Datatype 'Some missing datatype' is not supported by OpenMRS");
		importer.importConcept(new CacheService(conceptService), update, concept);
	}
	
	/**
	 * @see Importer#importConcept(OclConcept,ImportQueue)
	 * @verifies change duplicate synonym to index term
	 */
	@Test
	public void importConcept_shouldChangeDuplicateSynonymToIndexTerm() throws Exception {
		Update update = updateService.getLastUpdate();
		
		OclConcept concept = newOclConcept();
		
		Name polishName = new Name();
		polishName.setExternalId(UUID.randomUUID().toString());
		polishName.setName("Nazwa");
		polishName.setLocale(new Locale("pl"));
		polishName.setNameType("FULLY_SPECIFIED");
		polishName.setLocalePreferred(true);
		concept.getNames().add(polishName);
		
		importer.importConcept(new CacheService(conceptService), update, concept);
		
		OclConcept conceptWithSynonym = newOtherOclConcept();
		Name otherPolishName = new Name();
		otherPolishName.setExternalId(UUID.randomUUID().toString());
		otherPolishName.setName("Nazwa");
		otherPolishName.setLocale(new Locale("pl"));
		otherPolishName.setLocalePreferred(true);
		conceptWithSynonym.getNames().add(otherPolishName);
		
		importer.importConcept(new CacheService(conceptService), update, conceptWithSynonym);
		
		Concept importedConcept = conceptService.getConceptByUuid(concept.getExternalId());
		Concept importedConceptWithIndexTerm = conceptService.getConceptByUuid(conceptWithSynonym.getExternalId());
		
		assertThat(importedConcept.getNames(), hasItem((Matcher<? super ConceptName>) allOf(hasProperty("conceptNameType", equalTo(ConceptNameType.FULLY_SPECIFIED)),
			hasProperty("name", equalTo("Nazwa")))));
		
		assertThat(importedConceptWithIndexTerm.getNames(), hasItem((Matcher<? super ConceptName>) allOf(hasProperty("conceptNameType", equalTo(ConceptNameType.INDEX_TERM)),
			hasProperty("name", equalTo("Nazwa")))));
	}
	
	@Test
	public void importMapping_shouldAddConceptAnswer() throws Exception {
		Update update = updateService.getLastUpdate();
		
		OclConcept question = newOclConcept();
		updateService.saveItem(importer.importConcept(new CacheService(conceptService), update, question));
		
		OclConcept answer = newOtherOclConcept();
		updateService.saveItem(importer.importConcept(new CacheService(conceptService), update, answer));
		
		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		
		oclMapping.setMapType(MapType.Q_AND_A);
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/");
		
		importer.importMapping(new CacheService(conceptService), update, oclMapping);
		
		Concept questionConcept = conceptService.getConceptByUuid(question.getExternalId());
		Concept answerConcept = conceptService.getConceptByUuid(answer.getExternalId());
		
		assertThat(questionConcept.getAnswers(), contains(hasQuestionAndAnswer(questionConcept, answerConcept)));
	}
	
	@Test
	public void importMapping_shouldRemoveConceptAnswer() throws Exception {
		importMapping_shouldAddConceptAnswer();
		
		Update update = updateService.getLastUpdate();
		
		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		
		oclMapping.setMapType(MapType.Q_AND_A);
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/");
		oclMapping.setRetired(true);
		
		importer.importMapping(new CacheService(conceptService), update, oclMapping);
		
		Concept questionConcept = conceptService.getConceptByUuid("6c1bbb30-55f6-11e4-8ed6-0800200c9a66");
		
		assertThat(questionConcept.getAnswers(), is(empty()));
	}
	
	@Test
	public void importMapping_shouldAddConceptSetMemeber() throws Exception {
		Update update = updateService.getLastUpdate();
		
		OclConcept set = newOclConcept();
		updateService.saveItem(importer.importConcept(new CacheService(conceptService), update, set));
		
		OclConcept member = newOtherOclConcept();
		updateService.saveItem(importer.importConcept(new CacheService(conceptService), update, member));
		
		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		
		oclMapping.setMapType(MapType.SET);
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/");
		
		importer.importMapping(new CacheService(conceptService), update, oclMapping);
		
		Concept setConcept = conceptService.getConceptByUuid(set.getExternalId());
		Concept memberConcept = conceptService.getConceptByUuid(member.getExternalId());
		
		assertThat(setConcept.getSetMembers(), contains(memberConcept));
	}
	
	@Test
	public void importMapping_shouldRemoveConceptSetMemeber() throws Exception {
		importMapping_shouldAddConceptSetMemeber();
		
		Update update = updateService.getLastUpdate();
		
		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		
		oclMapping.setMapType(MapType.SET);
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1002/");
		oclMapping.setRetired(true);
		
		importer.importMapping(new CacheService(conceptService), update, oclMapping);
		
		Concept setConcept = conceptService.getConceptByUuid("6c1bbb30-55f6-11e4-8ed6-0800200c9a66");
		
		assertThat(setConcept.getSetMembers(), is(empty()));
	}
	
	@Test
	public void importMapping_shouldAddConceptMappingAndTerm() throws Exception {
		Update update = updateService.getLastUpdate();
		
		OclConcept oclConcept = newOclConcept();
		updateService.saveItem(importer.importConcept(new CacheService(conceptService), update, oclConcept));
		
		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		
		oclMapping.setMapType("SAME-AS");
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToSourceName("SNOMED CT");
		oclMapping.setToConceptCode("1001");
		
		importer.importMapping(new CacheService(conceptService), update, oclMapping);
		
		Concept concept = conceptService.getConceptByUuid("6c1bbb30-55f6-11e4-8ed6-0800200c9a66");
		
		ConceptSource source = conceptService.getConceptSourceByName("SNOMED CT");
		ConceptMapType mapType = conceptService.getConceptMapTypeByName("SAME_AS");
		assertThat(concept.getConceptMappings(), contains(hasMapping(source, "1001", mapType)));
	}
	
	@Test
	public void importMapping_shouldRemoveConceptMappingAndRetireTerm() throws Exception {
		importMapping_shouldAddConceptMappingAndTerm();
		
		Update update = updateService.getLastUpdate();
		
		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		
		oclMapping.setMapType("SAME-AS");
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToSourceName("SNOMED CT");
		oclMapping.setToConceptCode("1001");
		oclMapping.setRetired(true);
		
		importer.importMapping(new CacheService(conceptService), update, oclMapping);
		
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
		
		Update update = updateService.getLastUpdate();
		
		OclMapping oclMapping = new OclMapping();
		oclMapping.setExternalId("dde0d8cb-b44b-4901-90e6-e5066488814f");
		
		oclMapping.setMapType("SAME-AS");
		oclMapping.setFromConceptUrl("/orgs/CIELTEST/sources/CIELTEST/concepts/1001/");
		oclMapping.setToSourceName("SNOMED CT");
		oclMapping.setToConceptCode("1001");
		oclMapping.setRetired(false);
		
		importer.importMapping(new CacheService(conceptService), update, oclMapping);
		
		Concept concept = conceptService.getConceptByUuid("6c1bbb30-55f6-11e4-8ed6-0800200c9a66");
		
		ConceptSource source = conceptService.getConceptSourceByName("SNOMED CT");
		ConceptMapType mapType = conceptService.getConceptMapTypeByName("SAME_AS");
		assertThat(concept.getConceptMappings(), contains(hasMapping(source, "1001", mapType)));
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
		oclConcept.setDescriptons(descriptons);
		
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
		oclConcept.setDescriptons(descriptons);
		
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
	
	private Concept assertImported(OclConcept oclConcept) {
		Concept concept = conceptService.getConceptByUuid(oclConcept.getExternalId());
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
