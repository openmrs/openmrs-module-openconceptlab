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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.spi.RootLogger;
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
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.openconceptlab.client.OclClient;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclMapping;
import org.openmrs.module.openconceptlab.importer.Importer;
import org.openmrs.module.openconceptlab.importer.Saver;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipFile;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.openmrs.module.openconceptlab.client.OclClient.FILE_NAME_FORMAT;

public class ImportServiceTest extends BaseModuleContextSensitiveTest {

	@Autowired @Qualifier("openconceptlab.importService")
	private ImportService importService;

    @Mock
    private ImportService mockedUpdateService;

    @Autowired
    private ConceptService conceptService;

	@Autowired
	private Saver saver;

	@Mock
	private OclClient mockedOclClient;

	@Mock
	private GetMethod mockedGetMethod;

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
	 * @see ImportServiceImpl#getImportsInOrder(int, int)
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
	 * @Verifies prepend api. to the url's host and saves the subscription
	 */
	@Test
	public void saveSubscription_shouldPrependApiAndSaveSubscription() throws Exception {
		Subscription newSubscription = new Subscription();
		newSubscription.setUrl("http://openconceptlab.com");
		newSubscription.setDays(5);
		newSubscription.setHours(3);
		newSubscription.setMinutes(30);
		newSubscription.setToken("c84e5a66d8b2e9a9bf1459cd81e6357f1c6a997e");

		importService.saveSubscription(newSubscription);

		Subscription subscription = importService.getSubscription();
		assertThat(subscription.getUrl(), is("http://api.openconceptlab.com"));
	}

	/**
	 * @see ImportServiceImpl#saveSubscription(Subscription)
	 * @Verifies saves the subscription
	 */
	@Test
	public void saveSubscription_shouldSaveSubscription() throws Exception {
		Subscription newSubscription = new Subscription();
		newSubscription.setUrl("http://api.openconceptlab.com/");
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

	@Ignore("This test passes locally, but fails on Travis-CI")
	@Transactional(propagation = Propagation.NEVER)
	@Test
	public void update_shouldDoInitialUpdate() throws Exception {
		final String conceptUuid = "159947AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		final String subscriptionUrl = "https://api.staging.openconceptlab.org/orgs/openmrs/collections/reference-application";
		final String subscriptionToken = "53fc72f0498a707a26e4d903c0f24c2db24d1e35";
		final String testResourcePath = "refapp-collection-response-v1/response.zip";
		final String version = "v1.0-beta.1";
		final String dateHeaderValue = "Tue, 29 Nov 2016 17:01:58 GMT";
		final String contentTypeHeaderValue = "application/zip";

		File responseZip = null;
		Concept concept = null;
    	try {
			Subscription subscription = new Subscription();
			subscription.setUrl(subscriptionUrl);
			subscription.setToken(subscriptionToken);

			importService.saveSubscription(subscription);
			Importer importer = new Importer();

			InputStream response = TestResources.getResponseAsStream(testResourcePath);

			when(mockedGetMethod.getResponseHeader("Date")).thenReturn(new Header("Date", dateHeaderValue));
			when(mockedGetMethod.getResponseHeader("Content-Type")).thenReturn(new Header("Content-Type", contentTypeHeaderValue));
			when(mockedGetMethod.getResponseBodyAsStream()).thenReturn(response);
			when(mockedGetMethod.getResponseContentLength()).thenReturn(Long.valueOf(response.available()));

			when(mockedOclClient.executeExportRequest(importService.getSubscription().getUrl(), version, importService.getSubscription().getToken())).thenReturn(mockedGetMethod);
			when(mockedOclClient.fetchOclConcepts(importService.getSubscription().getUrl(),importService.getSubscription().getToken())).thenCallRealMethod();
			when(mockedOclClient.fetchLatestOclReleaseVersion(importService.getSubscription().getUrl(),importService.getSubscription().getToken())).thenReturn(version);

			SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
			Date date = format.parse(dateHeaderValue);
			SimpleDateFormat fileNameFormat = new SimpleDateFormat(FILE_NAME_FORMAT);
			responseZip = File.createTempFile(fileNameFormat.format(date), ".zip");
			when(mockedOclClient.newFile(date)).thenReturn(responseZip);

			importer.setOclClient(mockedOclClient);
			importer.setImportService(importService);
			importer.setSaver(saver);
			importer.setConceptService(conceptService);

			TestResources.setupDaemonToken();

			if (conceptService.getConceptByUuid(conceptUuid) != null) {
				conceptService.purgeConcept(conceptService.getConceptByUuid(conceptUuid));
			}

			importer.runTask();

			concept = conceptService.getConceptByUuid(conceptUuid);
			assertThat(concept, is(notNullValue()));

		}
		finally {
    		if (concept != null) {
				conceptService.purgeConcept(concept);
			}
			if (responseZip != null && responseZip.exists()) {
				responseZip.delete();
			}
    		importService.unsubscribe();
		}
	}

	@Ignore("This test passes locally, but fails on Travis-CI")
	@Transactional(propagation = Propagation.NEVER)
	@Test
	public void update_shouldDoFollowupUpdate() throws Exception {
    	final String initialConceptUuid = "159947AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    	final String followupConceptUuid = "1002AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    	final String subscriptionUrl = "https://api.staging.openconceptlab.org/orgs/openmrs/collections/reference-application/";
		final String subscriptionToken = "53fc72f0498a707a26e4d903c0f24c2db24d1e35";
		final String initialResponseTestResourcePath = "refapp-collection-response-v1/response.zip";
		final String followupResponseTestResourcePath = "refapp-collection-response-v2/response.zip";
		final String initialVersion = "v1.0-beta.1";
		final String followupVersion = "v1.0-beta.2";
		String initialDateHeaderValue = "Tue, 29 Nov 2016 17:01:58 GMT";
		String followupDateHeaderValue = "Tue, 29 Nov 2016 18:01:58 GMT";
		String contentTypeHeaderValue = "application/zip";

		Concept initialConcept = null;
		Concept followupConcept = null;
		File initialResponseZip = null;
		File followupResponseZip = null;
		try {
			//INITIAL UPDATE
			Subscription subscription = new Subscription();
			subscription.setUrl(subscriptionUrl);
			subscription.setToken(subscriptionToken);

			importService.saveSubscription(subscription);
			Importer importer = new Importer();

			InputStream initialResponse = TestResources.getResponseAsStream(initialResponseTestResourcePath);

			when(mockedGetMethod.getResponseHeader("Date")).thenReturn(new Header("Date", initialDateHeaderValue));
			when(mockedGetMethod.getResponseHeader("Content-Type")).thenReturn(new Header("Content-Type", contentTypeHeaderValue));
			when(mockedGetMethod.getResponseBodyAsStream()).thenReturn(initialResponse);
			when(mockedGetMethod.getResponseContentLength()).thenReturn(Long.valueOf(initialResponse.available()));

			when(mockedOclClient.executeExportRequest(importService.getSubscription().getUrl(), initialVersion, importService.getSubscription().getToken())).thenReturn(mockedGetMethod);
			when(mockedOclClient.fetchOclConcepts(importService.getSubscription().getUrl(),importService.getSubscription().getToken())).thenCallRealMethod();
			when(mockedOclClient.fetchLatestOclReleaseVersion(importService.getSubscription().getUrl(),importService.getSubscription().getToken())).thenReturn(initialVersion);

			SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
			Date date = format.parse(initialDateHeaderValue);
			SimpleDateFormat fileNameFormat = new SimpleDateFormat(FILE_NAME_FORMAT);
			initialResponseZip = File.createTempFile(fileNameFormat.format(date), ".zip");
			when(mockedOclClient.newFile(date)).thenReturn(initialResponseZip);

			importer.setOclClient(mockedOclClient);
			importer.setImportService(importService);
			importer.setSaver(saver);
			importer.setConceptService(conceptService);

			TestResources.setupDaemonToken();

			if (conceptService.getConceptByUuid(initialConceptUuid) != null) {
				conceptService.purgeConcept(conceptService.getConceptByUuid(initialConceptUuid));
			}

			importer.runTask();

			initialConcept = conceptService.getConceptByUuid(initialConceptUuid);
			assertThat(initialConcept, is(notNullValue()));

			//FOLLOWUP UPDATE
			InputStream followupResponse = TestResources.getResponseAsStream(followupResponseTestResourcePath);

			when(mockedGetMethod.getResponseHeader("Date")).thenReturn(new Header("Date", followupDateHeaderValue));
			when(mockedGetMethod.getResponseHeader("Content-Type")).thenReturn(new Header("Content-Type", contentTypeHeaderValue));
			when(mockedGetMethod.getResponseBodyAsStream()).thenReturn(followupResponse);
			when(mockedGetMethod.getResponseContentLength()).thenReturn(Long.valueOf(followupResponse.available()));

			when(mockedOclClient.executeExportRequest(importService.getSubscription().getUrl(), followupVersion, importService.getSubscription().getToken())).thenReturn(mockedGetMethod);
			when(mockedOclClient.fetchOclConcepts(importService.getSubscription().getUrl(),importService.getSubscription().getToken(), initialVersion)).thenCallRealMethod();
			when(mockedOclClient.fetchLatestOclReleaseVersion(importService.getSubscription().getUrl(),importService.getSubscription().getToken())).thenReturn(followupVersion);

			date = format.parse(followupDateHeaderValue);
			followupResponseZip = File.createTempFile(fileNameFormat.format(date), ".zip");
			when(mockedOclClient.newFile(date)).thenReturn(followupResponseZip);

			if (conceptService.getConceptByUuid(followupConceptUuid) != null) {
				conceptService.purgeConcept(conceptService.getConceptByUuid(followupConceptUuid));
			}

			importer.runTask();

			followupConcept = conceptService.getConceptByUuid(followupConceptUuid);
			assertThat(followupConcept, is(notNullValue()));
		}
		finally {
			if (initialConcept != null) {
				conceptService.purgeConcept(initialConcept);
			}
			if (followupConcept != null) {
				conceptService.purgeConcept(followupConcept);
			}
			if (initialResponseZip != null && initialResponseZip.exists()) {
				initialResponseZip.delete();
			}
			if (followupResponseZip != null && followupResponseZip.exists()) {
				followupResponseZip.delete();
			}
			importService.unsubscribe();
		}
	}

	@Ignore("This test passes locally, but fails on Travis-CI")
	@Transactional(propagation = Propagation.NEVER)
	@Test
	public void update_shouldNotRunFollowupUpdateWhenVersionDidNotChange() throws Exception {
		final String conceptUuid = "159947AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		final String subscriptionUrl = "https://api.staging.openconceptlab.org/orgs/openmrs/collections/reference-application/";
		final String subscriptionToken = "53fc72f0498a707a26e4d903c0f24c2db24d1e35";
		final String testResourcePath = "refapp-collection-response-v1/response.zip";
		final String version = "v1.0-beta.1";
		final String dateHeaderValue = "Tue, 29 Nov 2016 17:01:58 GMT";
		final String contentTypeHeaderValue = "application/zip";

		File responseZip = null;
		Concept initialConcept = null;
		try {
			//INITIAL UPDATE
			Subscription subscription = new Subscription();
			subscription.setUrl(subscriptionUrl);
			subscription.setToken(subscriptionToken);

			importService.saveSubscription(subscription);
			Importer importer = new Importer();

			InputStream response = TestResources.getResponseAsStream(testResourcePath);

			when(mockedGetMethod.getResponseHeader("Date")).thenReturn(new Header("Date", dateHeaderValue));
			when(mockedGetMethod.getResponseHeader("Content-Type")).thenReturn(new Header("Content-Type", contentTypeHeaderValue));
			when(mockedGetMethod.getResponseBodyAsStream()).thenReturn(response);
			when(mockedGetMethod.getResponseContentLength()).thenReturn(Long.valueOf(response.available()));

			when(mockedOclClient.executeExportRequest(importService.getSubscription().getUrl(), version, importService.getSubscription().getToken())).thenReturn(mockedGetMethod);
			when(mockedOclClient.fetchOclConcepts(importService.getSubscription().getUrl(),importService.getSubscription().getToken())).thenCallRealMethod();
			when(mockedOclClient.fetchLatestOclReleaseVersion(importService.getSubscription().getUrl(),importService.getSubscription().getToken())).thenReturn(version);

			SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
			Date date = format.parse(dateHeaderValue);
			SimpleDateFormat fileNameFormat = new SimpleDateFormat(FILE_NAME_FORMAT);
			responseZip = File.createTempFile(fileNameFormat.format(date), ".zip");
			when(mockedOclClient.newFile(date)).thenReturn(responseZip);

			importer.setOclClient(mockedOclClient);
			importer.setImportService(importService);
			importer.setSaver(saver);
			importer.setConceptService(conceptService);

			TestResources.setupDaemonToken();

			if (conceptService.getConceptByUuid(conceptUuid) != null) {
				conceptService.purgeConcept(conceptService.getConceptByUuid(conceptUuid));
			}

			importer.runTask();

			initialConcept = conceptService.getConceptByUuid(conceptUuid);
			assertThat(initialConcept, is(notNullValue()));

			conceptService.purgeConcept(initialConcept);

			importer.runTask();

			initialConcept = conceptService.getConceptByUuid(conceptUuid);
			assertThat(initialConcept, is(nullValue()));
		}
		finally {
			if (initialConcept != null) {
				conceptService.purgeConcept(initialConcept);
			}
			if (responseZip != null && responseZip.exists()) {
				responseZip.delete();
			}
		}
	}

    //ONLINE INTEGRATION TEST
	@Ignore("This test passes locally, but fails on Travis-CI")
	@Transactional(propagation = Propagation.NEVER)
    @Test
	public void update_shouldFetchLatestReferenceApplicationCollectionConcepts() throws Exception {
		Concept concept = null;
    	try {
			Subscription subscription = new Subscription();
			subscription.setUrl("https://api.staging.openconceptlab.org/orgs/openmrs/collections/reference-application");
			subscription.setToken("53fc72f0498a707a26e4d903c0f24c2db24d1e35");
			importService.saveSubscription(subscription);

			File tempDir = File.createTempFile("ocl", "");
			FileUtils.deleteQuietly(tempDir);
			tempDir.deleteOnExit();
			OclClient oclClient = new OclClient(tempDir.getAbsolutePath());

			Importer importer = new Importer();
			importer.setImportService(importService);
			importer.setConceptService(conceptService);
			importer.setSaver(saver);
			importer.setOclClient(oclClient);

			TestResources.setupDaemonToken();

			if (conceptService.getConceptByUuid("159947AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") != null) {
				conceptService.purgeConcept(conceptService.getConceptByUuid("159947AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
			}

			importer.run();

			concept = conceptService.getConceptByUuid("159947AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

			assertThat(concept, is(notNullValue()));
		}
		finally {
    		if (concept != null) {
				conceptService.purgeConcept(concept);
			}
			importService.unsubscribe();
		}
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

	@Test
	public void run_shouldSetSubscriptionUrlForLocalFilePath() throws IOException, URISyntaxException {
		ZipFile zipFile = TestResources.getSimpleZipFile();
		Level rootLoggerLevel = RootLogger.getRootLogger().getLevel();
		try {
			RootLogger.getRootLogger().setLevel(Level.OFF);
			Importer importer = new Importer();
			importer.setImportService(importService);
			importer.setConceptService(conceptService);
			importer.setSaver(saver);

			TestResources.setupDaemonToken();
			importer.run(zipFile);

			Import lastImport = importService.getLastImport();

			assertEquals(Context.getAdministrationService()
							.getGlobalProperty(OpenConceptLabConstants.GP_OCL_LOAD_AT_STARTUP_PATH),
					lastImport.getSubscriptionUrl());
		} finally {
			RootLogger.getRootLogger().setLevel(rootLoggerLevel);
		}
	}

	/**
	 * @see ImportServiceImpl#saveItems(Iterable)
	 * @verifies persist all items in a batch larger than BATCH_SIZE
	 */
	@Test
	public void saveItems_shouldPersistAllItemsAcrossBatchBoundary() throws Exception {
		Import anImport = new Import();
		importService.startImport(anImport);

		int itemCount = Importer.BATCH_SIZE + 50;
		List<Item> items = new ArrayList<>(itemCount);
		for (int i = 0; i < itemCount; i++) {
			OclConcept oclConcept = new OclConcept();
			oclConcept.setUrl("/orgs/test/sources/test/concepts/concept-" + i + "/");
			oclConcept.setVersionUrl(oclConcept.getUrl() + "v1/");
			oclConcept.setExternalId(UUID.randomUUID().toString());
			items.add(new Item(anImport, oclConcept, ItemState.ADDED));
		}

		importService.saveItems(items);
		importService.flushAndClearSession();

		Set<ItemState> allStates = new HashSet<>(EnumSet.allOf(ItemState.class));
		Integer persistedCount = importService.getImportItemsCount(anImport, allStates);
		assertEquals(Integer.valueOf(itemCount), persistedCount);

		List<Item> persisted = importService.getImportItems(anImport, 0, itemCount, Collections.<ItemState>emptySet());
		assertEquals(itemCount, persisted.size());
		for (Item item : persisted) {
			assertThat(item.getItemId(), is(notNullValue()));
			assertThat(item.getUuid(), is(notNullValue()));
			assertThat(item.getUrl(), is(notNullValue()));
			assertThat(item.getVersionUrl(), is(notNullValue()));
			assertThat(item.getHashedUrl(), is(notNullValue()));
			assertEquals(ItemType.CONCEPT, item.getType());
			assertEquals(ItemState.ADDED, item.getState());
		}
	}

	/**
	 * saveItems must persist ERROR-state items with the errorMessage column populated and the
	 * value preserved through round-trip even at lengths approaching the 1024-char column cap.
	 */
	@Test
	public void saveItems_shouldPersistErrorItemsWithErrorMessage() {
		Import anImport = new Import();
		importService.startImport(anImport);

		String longMessage = StringUtils.repeat("err-", 250); // 1000 chars, under the 1024 column cap
		OclConcept oclConcept = new OclConcept();
		oclConcept.setUrl("/orgs/test/sources/test/concepts/err-concept/");
		oclConcept.setVersionUrl(oclConcept.getUrl() + "v1/");
		oclConcept.setExternalId(UUID.randomUUID().toString());
		Item errorItem = new Item(anImport, oclConcept, ItemState.ERROR);
		errorItem.setErrorMessage(longMessage);

		importService.saveItems(Collections.singletonList(errorItem));
		importService.flushAndClearSession();

		List<Item> persisted = importService.getImportItems(anImport, 0, 10,
				new HashSet<>(EnumSet.of(ItemState.ERROR)));
		assertEquals(1, persisted.size());
		assertEquals(longMessage, persisted.get(0).getErrorMessage());
		assertEquals(ItemState.ERROR, persisted.get(0).getState());
	}

	/**
	 * MAPPING items carry {@code updatedOn} populated from {@link OclMapping#getUpdatedOn()}; ensure
	 * the column round-trips through saveItems for the MAPPING type branch.
	 */
	@Test
	public void saveItems_shouldPersistMappingItemsWithUpdatedOn() throws Exception {
		Import anImport = new Import();
		importService.startImport(anImport);

		// Use a date with zero millis so round-trip equality holds regardless of whether the
		// underlying DATETIME column stores fractional seconds.
		Date updatedOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2024-06-01 12:30:45");

		List<Item> items = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			OclMapping oclMapping = new OclMapping();
			oclMapping.setUrl("/orgs/test/sources/test/mappings/map-" + i + "/");
			oclMapping.setExternalId(UUID.randomUUID().toString());
			oclMapping.setUpdatedOn(updatedOn);
			items.add(new Item(anImport, oclMapping, ItemState.ADDED));
		}

		importService.saveItems(items);
		importService.flushAndClearSession();

		List<Item> persisted = importService.getImportItems(anImport, 0, 100, Collections.<ItemState>emptySet());
		assertEquals(5, persisted.size());
		for (Item p : persisted) {
			assertEquals(ItemType.MAPPING, p.getType());
			assertNotNull("updatedOn must round-trip for MAPPING items", p.getUpdatedOn());
			assertEquals(updatedOn.getTime() / 1000, p.getUpdatedOn().getTime() / 1000);
		}
	}

	/**
	 * saveItems must surface a constraint violation as a thrown exception, not silently swallow
	 * it. Triggered here via a FK violation on {@code openconceptlab_item.import_id}: items
	 * reference an Import whose id points to no real row.
	 */
	@Test
	public void saveItems_shouldThrowOnConstraintViolation() throws Exception {
		Import bogus = new Import();
		Field idField = Import.class.getDeclaredField("importId");
		idField.setAccessible(true);
		idField.set(bogus, 999999999L);

		List<Item> items = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			OclConcept oclConcept = new OclConcept();
			oclConcept.setUrl("/orgs/test/sources/test/concepts/cv-" + i + "/");
			oclConcept.setVersionUrl(oclConcept.getUrl() + "v1/");
			oclConcept.setExternalId(UUID.randomUUID().toString());
			items.add(new Item(bogus, oclConcept, ItemState.ADDED));
		}

		Throwable thrown = null;
		try {
			importService.saveItems(items);
			importService.flushAndClearSession();
		} catch (Throwable t) {
			thrown = t;
		}
		assertNotNull("Expected an exception when import_id refers to a non-existent row", thrown);
	}

	/**
	 * Defensive guard: saveItems with empty input must be a no-op, not fall through into an
	 * invalid statement or NPE.
	 */
	@Test
	public void saveItems_shouldBeNoOpForEmptyInput() {
		Import anImport = new Import();
		importService.startImport(anImport);

		importService.saveItems(Collections.<Item>emptyList());
		importService.flushAndClearSession();

		Integer count = importService.getImportItemsCount(anImport,
				new HashSet<>(EnumSet.allOf(ItemState.class)));
		assertEquals(Integer.valueOf(0), count);
	}

	/**
	 * Second-import scenario: when an import has already completed once, the Importer must NOT
	 * set {@code skipDbItemLookups(true)} on the cache; otherwise a re-import of the same OCL
	 * payload would re-add already-imported concepts. Observable contract: the second run records
	 * every concept as {@link ItemState#UP_TO_DATE} rather than {@link ItemState#ADDED}.
	 */
	@Test
	public void importer_shouldRecognizeAlreadyImportedConceptsOnSecondRun() throws Exception {
		int itemCount = 3;
		// Generate test-scoped uuids and urls so both runs in this test see identical inputs but
		// other tests' leftover daemon-committed data can't collide with ours.
		String runId = UUID.randomUUID().toString();
		List<String> conceptUuidPlan = new ArrayList<>(itemCount);
		List<String> conceptUrlPlan = new ArrayList<>(itemCount);
		for (int i = 0; i < itemCount; i++) {
			conceptUuidPlan.add(UUID.randomUUID().toString());
			conceptUrlPlan.add("/orgs/test/sources/test/concepts/" + runId + "-c" + i + "/");
		}

		File firstZip = null;
		File secondZip = null;
		Set<String> conceptUuids = new HashSet<>();
		Set<Long> importIdsToPurge = new HashSet<>();
		try {
			firstZip = buildIdenticalOclZip(conceptUuidPlan, conceptUrlPlan);

			Importer importer = new Importer();
			importer.setImportService(importService);
			importer.setConceptService(conceptService);
			importer.setSaver(saver);

			TestResources.setupDaemonToken();

			// First import: every concept is ADDED.
			try (ZipFile zip = new ZipFile(firstZip)) {
				importer.run(zip);
			}
			Import firstImport = importService.getLastImport();
			assertNotNull(firstImport);
			importIdsToPurge.add(firstImport.getImportId());
			Integer addedFirst = importService.getImportItemsCount(firstImport,
					new HashSet<>(EnumSet.of(ItemState.ADDED)));
			assertEquals("first import should ADD every concept", Integer.valueOf(itemCount), addedFirst);

			for (Item item : importService.getImportItems(firstImport, 0, itemCount + 10,
					new HashSet<>(EnumSet.of(ItemState.ADDED)))) {
				conceptUuids.add(item.getUuid());
			}

			// Build a second zip file (Importer deletes the input zip after a successful run).
			secondZip = buildIdenticalOclZip(conceptUuidPlan, conceptUrlPlan);
			try (ZipFile zip = new ZipFile(secondZip)) {
				importer.run(zip);
			}

			Import secondImport = importService.getLastImport();
			assertNotNull(secondImport);
			importIdsToPurge.add(secondImport.getImportId());
			assertFalse("first and second imports must be distinct rows",
					firstImport.getImportId().equals(secondImport.getImportId()));

			// If skipDbItemLookups were true, every concept would be re-ADDED. The fact that the
			// importer detected previous-import state means it queried the DB for prior items.
			Integer upToDateSecond = importService.getImportItemsCount(secondImport,
					new HashSet<>(EnumSet.of(ItemState.UP_TO_DATE)));
			assertEquals("second import should treat already-imported concepts as UP_TO_DATE",
					Integer.valueOf(itemCount), upToDateSecond);
		} finally {
			purgeImportsAndConceptsInDaemonTransaction(importIdsToPurge, conceptUuids);
			if (firstZip != null && firstZip.exists()) {
				firstZip.delete();
			}
			if (secondZip != null && secondZip.exists()) {
				secondZip.delete();
			}
		}
	}

	/**
	 * In-batch error recovery: a single failing concept inside the importer's batch loop must not
	 * abort the batch — surrounding concepts must still be saved, and the failing one must be
	 * recorded as an {@link ItemState#ERROR} item with an {@code errorMessage}. Triggered via a
	 * synthetic zip that includes one concept with an unknown datatype.
	 */
	@Test
	public void importer_shouldRecordErrorForFailingConceptAndContinueBatch() throws Exception {
		int goodCount = 4;
		File zipFile = buildSyntheticOclZipWithOneBadConcept(goodCount);
		Set<String> conceptUuids = new HashSet<>();
		Set<Long> importIdsToPurge = new HashSet<>();
		try {
			Importer importer = new Importer();
			importer.setImportService(importService);
			importer.setConceptService(conceptService);
			importer.setSaver(saver);

			TestResources.setupDaemonToken();
			try (ZipFile zip = new ZipFile(zipFile)) {
				importer.run(zip);
			}

			Import lastImport = importService.getLastImport();
			assertNotNull(lastImport);
			importIdsToPurge.add(lastImport.getImportId());

			Integer added = importService.getImportItemsCount(lastImport,
					new HashSet<>(EnumSet.of(ItemState.ADDED)));
			Integer errors = importService.getImportItemsCount(lastImport,
					new HashSet<>(EnumSet.of(ItemState.ERROR)));
			assertEquals("all healthy concepts must still be added when one fails",
					Integer.valueOf(goodCount), added);
			assertEquals("the unhealthy concept must be recorded as ERROR", Integer.valueOf(1), errors);

			List<Item> errorItems = importService.getImportItems(lastImport, 0, 5,
					new HashSet<>(EnumSet.of(ItemState.ERROR)));
			assertEquals(1, errorItems.size());
			assertNotNull("ERROR item must carry an errorMessage", errorItems.get(0).getErrorMessage());
			assertTrue("errorMessage should mention the unknown datatype",
					errorItems.get(0).getErrorMessage().toLowerCase(Locale.ENGLISH).contains("datatype"));

			for (Item item : importService.getImportItems(lastImport, 0, goodCount + 5,
					new HashSet<>(EnumSet.of(ItemState.ADDED)))) {
				conceptUuids.add(item.getUuid());
			}
		} finally {
			purgeImportsAndConceptsInDaemonTransaction(importIdsToPurge, conceptUuids);
			if (zipFile != null && zipFile.exists()) {
				zipFile.delete();
			}
		}
	}

	/**
	 * Builds a zip with concepts that use the exact uuids/urls in the given lists. Re-running
	 * with the same inputs produces an identical zip, which is what the second-import test needs.
	 */
	private File buildIdenticalOclZip(List<String> uuids, List<String> urls) throws IOException {
		File zipFile = File.createTempFile("ocl-test", ".zip");
		try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
				new java.io.FileOutputStream(zipFile))) {
			zos.putNextEntry(new java.util.zip.ZipEntry("export.json"));
			StringBuilder json = new StringBuilder();
			// Importer.advanceToListOf() expects an OCL-shaped export: a top-level object with at
			// least one nested object (e.g. "extras") before "concepts". Mimic the real shape.
			json.append("{\"type\":\"Source\",\"extras\":{},\"concepts\":[");
			for (int i = 0; i < uuids.size(); i++) {
				if (i > 0) json.append(',');
				appendSyntheticConcept(json, uuids.get(i), urls.get(i), "Text", "Misc",
						"identical-test-" + i);
			}
			json.append("],\"mappings\":[]}");
			zos.write(json.toString().getBytes("UTF-8"));
			zos.closeEntry();
		}
		return zipFile;
	}

	/**
	 * Builds an export.json with {@code goodCount} valid concepts plus one bad concept (unknown
	 * datatype) inserted in the middle. Concept uuids and URLs are randomized per call to avoid
	 * collisions with leftover state from sibling tests — the Importer's daemon thread commits
	 * independently and Spring's test-rollback only undoes the test thread.
	 */
	private File buildSyntheticOclZipWithOneBadConcept(int goodCount) throws IOException {
		File zipFile = File.createTempFile("ocl-bad-concept-test", ".zip");
		String runId = UUID.randomUUID().toString();
		try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
				new java.io.FileOutputStream(zipFile))) {
			zos.putNextEntry(new java.util.zip.ZipEntry("export.json"));
			StringBuilder json = new StringBuilder();
			json.append("{\"type\":\"Source\",\"extras\":{},\"concepts\":[");

			int totalEmitted = 0;
			for (int i = 0; i < goodCount; i++) {
				if (totalEmitted > 0) json.append(',');
				appendSyntheticConcept(json, UUID.randomUUID().toString(),
						"/orgs/test/sources/test/concepts/" + runId + "-good-" + i + "/", "Text",
						"Misc", "good-concept-" + i);
				totalEmitted++;

				// Insert the bad concept in the middle of the run.
				if (i == goodCount / 2) {
					json.append(',');
					appendSyntheticConcept(json, UUID.randomUUID().toString(),
							"/orgs/test/sources/test/concepts/" + runId + "-bad/", "NotARealDatatype",
							"Misc", "bad-concept");
					totalEmitted++;
				}
			}

			json.append("],\"mappings\":[]}");
			zos.write(json.toString().getBytes("UTF-8"));
			zos.closeEntry();
		}
		return zipFile;
	}

	private void appendSyntheticConcept(StringBuilder json, String uuid, String url, String datatype,
			String conceptClass, String displayName) {
		String versionUrl = url + "v1/";
		json.append("{")
				.append("\"external_id\":\"").append(uuid).append("\",")
				.append("\"url\":\"").append(url).append("\",")
				.append("\"version_url\":\"").append(versionUrl).append("\",")
				.append("\"concept_class\":\"").append(conceptClass).append("\",")
				.append("\"datatype\":\"").append(datatype).append("\",")
				.append("\"retired\":false,")
				.append("\"names\":[{\"name\":\"").append(displayName)
				.append("\",\"locale\":\"en\",\"name_type\":\"FULLY_SPECIFIED\",\"locale_preferred\":true}],")
				.append("\"descriptions\":[]")
				.append("}");
	}

	/**
	 * Removes daemon-committed Imports (and their Items) plus the listed Concepts. Runs inside a
	 * daemon thread so the deletes happen in a fresh, committing transaction — without this, the
	 * test thread's @Transactional rollback would undo the cleanup and leave rows visible to
	 * subsequent tests. Failures are logged but not rethrown so they don't mask the real test
	 * outcome.
	 */
	private void purgeImportsAndConceptsInDaemonTransaction(final Set<Long> importIds,
			final Set<String> conceptUuids) {
		if (importIds.isEmpty() && conceptUuids.isEmpty()) {
			return;
		}
		try {
			Daemon.runInDaemonThreadAndWait(new Runnable() {
				@Override
				public void run() {
					for (String uuid : conceptUuids) {
						try {
							Concept c = conceptService.getConceptByUuid(uuid);
							if (c != null) {
								conceptService.purgeConcept(c);
							}
						} catch (Exception e) {
							System.err.println("Failed to purge concept " + uuid + ": " + e);
						}
					}
					for (Long id : importIds) {
						try {
							Context.getAdministrationService().executeSQL(
									"delete from openconceptlab_item where import_id = " + id, false);
							Context.getAdministrationService().executeSQL(
									"delete from openconceptlab_import where import_id = " + id, false);
						} catch (Exception e) {
							System.err.println("Failed to purge import " + id + ": " + e);
						}
					}
				}
			}, OpenConceptLabActivator.getDaemonToken());
		} catch (Exception e) {
			System.err.println("Daemon cleanup failed: " + e);
		}
	}
}
