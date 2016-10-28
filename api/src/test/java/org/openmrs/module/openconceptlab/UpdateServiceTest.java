/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab;


import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

public class UpdateServiceTest extends BaseModuleContextSensitiveTest {

	@Autowired
	ImportService updateService;

	@Autowired
	ConceptService conceptService;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	/**
	 * @see ImportServiceImpl#getImport(Long)
	 * @verifies return update with id
	 */
	@Test
	public void getUpdate_shouldReturnUpdateWithId() throws Exception {
		Import newUpdate = new Import();
		updateService.startImport(newUpdate);

		Import update = updateService.getImport(newUpdate.getImportId());

		assertThat(update, is(newUpdate));
	}

	/**
	 * @see ImportServiceImpl#getImport(Long)
	 * @verifies throw IllegalArgumentException if update does not exist
	 */
	@Test
	public void getUpdate_shouldThrowIllegalArgumentExceptionIfUpdateDoesNotExist() throws Exception {
		exception.expect(IllegalArgumentException.class);
		updateService.getImport(0L);

	}

	/**
	 * @see ImportServiceImpl#getUpdatesInOrder()
	 * @verifies return all updates ordered descending by ids
	 */
	@Test
	public void getUpdatesInOrder_shouldReturnAllUpdatesOrderedDescendingByIds() throws Exception {
		Import firstUpdate = new Import();
		updateService.startImport(firstUpdate);
		updateService.stopImport(firstUpdate);

		Import secondUpdate = new Import();
		updateService.startImport(secondUpdate);

		List<Import> updatesInOrder = updateService.getImportsInOrder(0, 20);

		assertThat(updatesInOrder, contains(secondUpdate, firstUpdate));
	}

	/**
     * @see ImportServiceImpl#startImport(Import)
     * @verifies throw IllegalStateException if another update is in progress
     */
    @Test
    public void scheduleUpdate_shouldThrowIllegalStateExceptionIfAnotherUpdateIsInProgress() throws Exception {
    	Import firstUpdate = new Import();
    	updateService.startImport(firstUpdate);

    	Import secondUpdate = new Import();
    	exception.expect(IllegalStateException.class);
    	updateService.startImport(secondUpdate);
    }

	/**
     * @see ImportServiceImpl#stopImport(Import)
     * @verifies throw IllegalArgumentException if not scheduled
     */
    @Test
    public void stopUpdate_shouldThrowIllegalArgumentExceptionIfNotScheduled() throws Exception {
    	Import update = new Import();
    	exception.expect(IllegalArgumentException.class);
    	updateService.stopImport(update);
    }

	/**
     * @see ImportServiceImpl#stopImport(Import)
     * @verifies throw IllegalStateException if trying to stop twice
     */
    @Test
    public void stopUpdate_shouldThrowIllegalStateExceptionIfTryingToStopTwice() throws Exception {
    	Import update = new Import();
    	updateService.startImport(update);
    	updateService.stopImport(update);

    	exception.expect(IllegalStateException.class);
    	updateService.stopImport(update);
    }

	/**
	 * @see ImportServiceImpl#saveSubscription(Subscription)
	 * @Verifies saves the subscription
	 */
	@Test
	public void saveSubscription_shouldSaveSubscription() throws Exception {
		Subscription newSubscription = new Subscription();
		newSubscription.setUrl("http://openconceptlab.com/");
		newSubscription.setDays(5);
		newSubscription.setHours(3);
		newSubscription.setMinutes(30);
		newSubscription.setToken("c84e5a66d8b2e9a9bf1459cd81e6357f1c6a997e");

		updateService.saveSubscription(newSubscription);

		Subscription subscription = updateService.getSubscription();
		assertThat(subscription, is(newSubscription));
	}

	@Test
	public void getDuplicateConceptNames_shoudlFindDuplicates() throws Exception {
		Concept concept = new Concept();
		concept.setDatatype(conceptService.getConceptDatatype(1));
		concept.setConceptClass(conceptService.getConceptClass(1));
		ConceptName conceptName = new ConceptName();
		conceptName.setName("Rubella Viêm não");
		conceptName.setConceptNameType(ConceptNameType.FULLY_SPECIFIED);
		conceptName.setLocale(new Locale("vi"));
		concept.addName(conceptName);
		conceptService.saveConcept(concept);

		Concept conceptToImport = new Concept();
		conceptToImport.setUuid(UUID.randomUUID().toString());
		ConceptName nameToImport = new ConceptName();
		nameToImport.setName("Rubella Viêm não");
		nameToImport.setLocale(new Locale("vi"));
		conceptToImport.addName(nameToImport);

		List<ConceptName> duplicateOclNames = updateService.changeDuplicateConceptNamesToIndexTerms(conceptToImport);
		assertThat(duplicateOclNames, contains((Matcher<? super ConceptName>) hasProperty("name", is("Rubella Viêm não"))));
	}
}