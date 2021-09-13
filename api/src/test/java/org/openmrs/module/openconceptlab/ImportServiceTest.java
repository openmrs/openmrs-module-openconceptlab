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
import org.apache.commons.io.IOUtils;
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
import org.openmrs.module.openconceptlab.client.OclClient;
import org.openmrs.module.openconceptlab.importer.Importer;
import org.openmrs.module.openconceptlab.importer.Saver;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.NotTransactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipFile;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.openmrs.module.openconceptlab.client.OclClient.FILE_NAME_FORMAT;

public class ImportServiceTest extends BaseModuleContextSensitiveTest {

	@Autowired
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

	@Override
	public Properties getRuntimeProperties() {
		Properties runtimeProperties = super.getRuntimeProperties();
		runtimeProperties.setProperty("hibernate.connection.url", "jdbc:h2:mem:openmrs;DB_CLOSE_DELAY=30;MVCC=true");
		return runtimeProperties;
	}

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
	@NotTransactional
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
	@NotTransactional
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
	@NotTransactional
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
	@NotTransactional
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
		RootLogger.getRootLogger().setLevel(Level.OFF);
		Importer importer = new Importer();
		importer.setImportService(importService);
		importer.setConceptService(conceptService);
		importer.setSaver(saver);

		TestResources.setupDaemonToken();
		importer.run(zipFile);

		Import lastImport = importService.getLastImport();

		assertEquals(Context.getAdministrationService().getGlobalProperty(OpenConceptLabConstants.GP_OCL_LOAD_AT_STARTUP_PATH),
				lastImport.getSubscriptionUrl());
		RootLogger.getRootLogger().setLevel(rootLoggerLevel);
	}

}
