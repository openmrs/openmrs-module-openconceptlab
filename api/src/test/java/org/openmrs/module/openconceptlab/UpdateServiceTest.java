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

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.module.openconceptlab.client.OclClient;
import org.openmrs.module.openconceptlab.importer.Importer;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class UpdateServiceTest extends BaseModuleContextSensitiveTest {

	@Autowired
	private ImportService importService;

    @Mock
    private ImportService mockedUpdateService;

    @Autowired
    private ConceptService conceptService;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	/**
	 * @see ImportServiceImpl#getImport(Long)
	 * @verifies return update with id
	 */
	@Test
	public void getUpdate_shouldReturnUpdateWithId() throws Exception {
		Import newUpdate = new Import();
		importService.startImport(newUpdate);

		Import update = importService.getImport(newUpdate.getImportId());

		assertThat(update, is(newUpdate));
	}

	/**
	 * @see ImportServiceImpl#getImport(Long)
	 * @verifies throw IllegalArgumentException if update does not exist
	 */
	@Test
	public void getUpdate_shouldThrowIllegalArgumentExceptionIfUpdateDoesNotExist() throws Exception {
		exception.expect(IllegalArgumentException.class);
		importService.getImport(0L);

	}

	/**
	 * @see ImportServiceImpl#getUpdatesInOrder()
	 * @verifies return all updates ordered descending by ids
	 */
	@Test
	public void getUpdatesInOrder_shouldReturnAllUpdatesOrderedDescendingByIds() throws Exception {
		Import firstUpdate = new Import();
		importService.startImport(firstUpdate);
		importService.stopImport(firstUpdate);

		Import secondUpdate = new Import();
		importService.startImport(secondUpdate);

		List<Import> updatesInOrder = importService.getImportsInOrder(0, 20);

		assertThat(updatesInOrder, contains(secondUpdate, firstUpdate));
	}

	/**
     * @see ImportServiceImpl#startImport(Import)
     * @verifies throw IllegalStateException if another update is in progress
     */
    @Test
    public void scheduleUpdate_shouldThrowIllegalStateExceptionIfAnotherUpdateIsInProgress() throws Exception {
    	Import firstUpdate = new Import();
    	importService.startImport(firstUpdate);

    	Import secondUpdate = new Import();
    	exception.expect(IllegalStateException.class);
    	importService.startImport(secondUpdate);
    }

	/**
     * @see ImportServiceImpl#stopImport(Import)
     * @verifies throw IllegalArgumentException if not scheduled
     */
    @Test
    public void stopUpdate_shouldThrowIllegalArgumentExceptionIfNotScheduled() throws Exception {
    	Import update = new Import();
    	exception.expect(IllegalArgumentException.class);
    	importService.stopImport(update);
    }

	/**
     * @see ImportServiceImpl#stopImport(Import)
     * @verifies throw IllegalStateException if trying to stop twice
     */
    @Test
    public void stopUpdate_shouldThrowIllegalStateExceptionIfTryingToStopTwice() throws Exception {
    	Import update = new Import();
    	importService.startImport(update);
    	importService.stopImport(update);

    	exception.expect(IllegalStateException.class);
    	importService.stopImport(update);
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
		newSubscription.setSubscribedToSnapshot(false);

		importService.saveSubscription(newSubscription);

		Subscription subscription = importService.getSubscription();
		assertThat(subscription, is(newSubscription));
	}

    /*
     * TODO:
     * These ignored tests are working fine,
     * but it takes too much time to finish them
     * since tests are downloading data from real OCL.
     * This issue will be fixed when there will be prepared
     * test data, which will be easier to fetch.
     */
    @Ignore("This test is used for update simulation")
	@Test
	public void startUpdate_shouldStartInitialUpdate() throws Exception {
		Subscription newSubscription = new Subscription();
		newSubscription.setUrl("http://api.openconceptlab.com/orgs/CIEL/sources/CIEL/");
		newSubscription.setToken("41062d8fd2bcf5eb5457988bbe2dcb5446a80a07");

		importService.saveSubscription(newSubscription);
        Importer importer = new Importer();

        File tempDir = File.createTempFile("ocl", "");
        FileUtils.deleteQuietly(tempDir);
        tempDir.deleteOnExit();
        OclClient oclClient = new OclClient(tempDir.getAbsolutePath());

        importer.setOclClient(oclClient);
        importer.setImportService(importService);

        importer.runTask();
	}

    @Ignore("This test is used for update simulation")
    @Test
    public void startUpdate_shouldStartReleaseUpdate() throws Exception {
        Subscription subscription = new Subscription();
        subscription.setUrl("http://api.openconceptlab.com/orgs/CIEL/sources/CIEL/");
        subscription.setToken("41062d8fd2bcf5eb5457988bbe2dcb5446a80a07");
        subscription.setSubscribedToSnapshot(false);

        Import anImport = new Import();
        anImport.setErrorMessage(null);
        anImport.setOclDateStarted(new Date());
        anImport.setReleaseVersion("some_outdated_version_v4.2.0");

        when(mockedUpdateService.getSubscription()).thenReturn(subscription);
        when(mockedUpdateService.getLastSuccessfulSubscriptionImport()).thenReturn(anImport);

        File tempDir = File.createTempFile("ocl", "");
        FileUtils.deleteQuietly(tempDir);
        tempDir.deleteOnExit();
        OclClient oclClient = new OclClient(tempDir.getAbsolutePath());

        Importer importer = new Importer();

        importer.setOclClient(oclClient);
        importer.setImportService(mockedUpdateService);

        importer.runTask();
    }

	@Ignore("This test is used for update simulation")
	@Test
    public void startUpdate_shouldStartSnapshotUpdate() throws Exception {
        Subscription subscription = new Subscription();
        subscription.setUrl("http://api.openconceptlab.com/orgs/CIEL/sources/CIEL/");
        subscription.setToken("41062d8fd2bcf5eb5457988bbe2dcb5446a80a07");
        subscription.setSubscribedToSnapshot(true);

        Import anImport = new Import();
        anImport.setErrorMessage(null);
        anImport.setOclDateStarted(new Date());
        anImport.setReleaseVersion("some_outdated_version_v4.2.0");

        when(mockedUpdateService.getSubscription()).thenReturn(subscription);
        when(mockedUpdateService.getLastSuccessfulSubscriptionImport()).thenReturn(anImport);

        File tempDir = File.createTempFile("ocl", "");
        FileUtils.deleteQuietly(tempDir);
        tempDir.deleteOnExit();
        OclClient oclClient = new OclClient(tempDir.getAbsolutePath());

        Importer updater = new Importer();

        updater.setOclClient(oclClient);
        updater.setImportService(mockedUpdateService);

        updater.runTask();
    }

    @Test
    public void update_shouldUpdateReleaseVersion() throws Exception {
        Import anImport = new Import();
        anImport.setOclDateStarted(new Date());
        final String version = "v1.2";
        importService.updateReleaseVersion(anImport, version);
        assertThat(version, is(importService.getLastSuccessfulSubscriptionImport().getReleaseVersion()));
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

		List<ConceptName> duplicateOclNames = importService.changeDuplicateConceptNamesToIndexTerms(conceptToImport);
		assertThat(duplicateOclNames, contains((Matcher<? super ConceptName>) hasProperty("name", is("Rubella Viêm não"))));
	}


}