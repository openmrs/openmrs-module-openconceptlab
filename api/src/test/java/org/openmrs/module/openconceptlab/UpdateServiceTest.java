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
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Spy;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.module.openconceptlab.client.OclClient;
import org.openmrs.module.openconceptlab.importer.Importer;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class UpdateServiceTest extends BaseModuleContextSensitiveTest {

	@Autowired
	private ImportService importService;

    @Autowired
    private Importer importer;

    @Mock
    private ImportService mockedImportService;

    @Autowired
    private ConceptService conceptService;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Spy
	private OclClient mockedOclClient;

	@Mock
	private GetMethod mockedGetMethod;

    @Before
    public void before() {
        TestResources.setupDaemonToken();
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
	 * @see ImportServiceImpl#getImportsInOrder()
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

		importService.saveSubscription(newSubscription);

		Subscription subscription = importService.getSubscription();
		assertThat(subscription, is(newSubscription));
	}

    /*
     * This is used to trigger real OCLM<->OCLAPI logic
     * @Ignored, because it takes too much time to fetch real data
     */
    @Ignore("This test is used to triggering real OCLM-OCLAPI logic")
	@Test
	public void startUpdate_shouldStartInitialUpdate() throws Exception {
		Subscription newSubscription = new Subscription();
		newSubscription.setUrl("http://api.openconceptlab.com/orgs/CIEL/sources/CIEL/");
		newSubscription.setToken("41062d8fd2bcf5eb5457988bbe2dcb5446a80a07");

		importService.saveSubscription(newSubscription);

        File tempDir = File.createTempFile("ocl", "");
        FileUtils.deleteQuietly(tempDir);
        tempDir.deleteOnExit();
        OclClient oclClient = new OclClient(tempDir.getAbsolutePath());

        importer.setOclClient(oclClient);
        importer.setImportService(importService);

        importer.runTask();
	}

    @Ignore("This test is used to triggering real OCLM-OCLAPI logic")
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

        when(mockedImportService.getSubscription()).thenReturn(subscription);
        when(mockedImportService.getLastSuccessfulSubscriptionImport()).thenReturn(anImport);

        File tempDir = File.createTempFile("ocl", "");
        FileUtils.deleteQuietly(tempDir);
        tempDir.deleteOnExit();
        OclClient oclClient = new OclClient(tempDir.getAbsolutePath());

        importer.setOclClient(oclClient);
        importer.setImportService(mockedImportService);

        importer.runTask();
    }

    @Ignore("This test is used to triggering real OCLM-OCLAPI logic")
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

        when(mockedImportService.getSubscription()).thenReturn(subscription);
        when(mockedImportService.getLastSuccessfulSubscriptionImport()).thenReturn(anImport);

        File tempDir = File.createTempFile("ocl", "");
        FileUtils.deleteQuietly(tempDir);
        tempDir.deleteOnExit();
        OclClient oclClient = new OclClient(tempDir.getAbsolutePath());

        importer.setOclClient(oclClient);
        importer.setImportService(mockedImportService);

        importer.runTask();
    }

	@Ignore("WIP - OCLM-46")
	@Test
	public void mockUpdate_shouldImportInitialDataFromMockedResponse() throws Exception {

		Subscription subscription = new Subscription();
		subscription.setUrl("http://api.openconceptlab.com/orgs/CIEL/sources/CIEL");
		subscription.setToken("41062d8fd2bcf5eb5457988bbe2dcb5446a80a07");

		final String filename = "CIEL_20150514-testdata.20150622121229.tar";

		final String releaseVersion = "1.2";
		final String exportUrl = "https://ocl-source-export-production.s3.amazonaws.com/CIEL/"
				+ filename
				+ "?Signature=p6C4oOtz%2Fgcom5PqXDDnFexeLfg%3D&Expires=1478508163&AWSAccessKeyId=AKIAJSVYSQTANHNWOOPQ";

        importService.saveSubscription(subscription);

        // Mock latest release version http request
		doReturn(releaseVersion).when(mockedOclClient).fetchLatestOclReleaseVersion(
                subscription.getUrl(),
                subscription.getToken());

		// Mock export url
		doReturn(exportUrl).when(mockedOclClient).fetchExportUrl(
                subscription.getUrl(),
                subscription.getToken(),
				releaseVersion);

		doReturn(mockedGetMethod).when(mockedOclClient).executeResponse(exportUrl);

		final String dateHeaderValue = "Mon, 07 Nov 2016 09:52:34 GMT";
		Header dateHeader = new Header();
		dateHeader.setName("Date");
		dateHeader.setValue(dateHeaderValue);

		// Mock response date header
		when(mockedGetMethod.getStatusCode()).thenReturn(200);
		when(mockedGetMethod.getResponseHeader("Date")).thenReturn(dateHeader);

		File oclResponseTarfile = new File("src/test/resources/", filename);
        ByteArrayInputStream fileInputStream = new ByteArrayInputStream(FileUtils.readFileToByteArray(oclResponseTarfile));

		// Mock response
		when(mockedGetMethod.getResponseBodyAsStream()).thenReturn(fileInputStream);
		when(mockedGetMethod.getResponseContentLength()).thenReturn(Long.valueOf(fileInputStream.available()));
		when(mockedGetMethod.getPath()).thenReturn(null);

		doReturn(new Date()).when(mockedOclClient).parseDateFromPath(null);

		importer.setOclClient(mockedOclClient);
		importer.setImportService(importService);

		importer.run();

        Concept concept = conceptService.getConceptByUuid("54ea96d28a86f20421474a3a");

        assertThat(concept, is(notNullValue()));
	}

    @Ignore("WIP - OCLM-46")
	@Test
	public void mockUpdate_shouldImportLatestReleaseMockedResponse() throws Exception {

		Subscription subscription = new Subscription();
		subscription.setUrl("http://api.openconceptlab.com/orgs/CIEL/sources/CIEL");
		subscription.setToken("41062d8fd2bcf5eb5457988bbe2dcb5446a80a07");
		subscription.setSubscribedToSnapshot(false);

		final String filename = "CIEL_20150514-testdata.20150622121229.tar";

		final String outdatedReleaseVersion = "1.2";
		final String actualReleaseVersion = "1.3";

		final String exportUrl = "https://ocl-source-export-production.s3.amazonaws.com/CIEL/"
				+ filename
				+ "?Signature=p6C4oOtz%2Fgcom5PqXDDnFexeLfg%3D&Expires=1478508163&AWSAccessKeyId=AKIAJSVYSQTANHNWOOPQ";

		//TODO: getSubscription() in ImportServiceImpl.java
		importService.saveSubscription(subscription);

		// Mock latest release version http request
		doReturn(outdatedReleaseVersion).when(mockedOclClient).fetchLatestOclReleaseVersion(
				importService.getSubscription().getUrl(),
				importService.getSubscription().getToken());

		// Mock export url
		doReturn(exportUrl).when(mockedOclClient).fetchExportUrl(
				importService.getSubscription().getUrl(),
				importService.getSubscription().getToken(),
				outdatedReleaseVersion);

		doReturn(mockedGetMethod).when(mockedOclClient).executeResponse(exportUrl);

		final String dateHeaderValue = "Mon, 07 Nov 2016 09:52:34 GMT";
		Header dateHeader = new Header();
		dateHeader.setName("Date");
		dateHeader.setValue(dateHeaderValue);

		// Mock response date header
		when(mockedGetMethod.getStatusCode()).thenReturn(200);
		when(mockedGetMethod.getResponseHeader("Date")).thenReturn(dateHeader);

		final String oclInitialResponseTarfilename = "CIEL_20150514-testdata.20150622121229.tar";
		File oclInitialResponseTarfile = new File("src/test/resources/", oclInitialResponseTarfilename);
		FileInputStream initialResponseFileInputStream = new FileInputStream(oclInitialResponseTarfile);

		// Mock response
		when(mockedGetMethod.getResponseBodyAsStream()).thenReturn(initialResponseFileInputStream);
		when(mockedGetMethod.getResponseContentLength()).thenReturn(Long.valueOf(initialResponseFileInputStream.available()));
		when(mockedGetMethod.getPath()).thenReturn(null);

		doReturn(new Date()).when(mockedOclClient).parseDateFromPath(null);

		//TODO Mock thread in ImportTask

		importer.setOclClient(mockedOclClient);
		importer.setImportService(importService);

		// Initial update
		importer.runTask();
		//TODO: Wait here?

		when(mockedOclClient.fetchLatestOclReleaseVersion(
				importService.getSubscription().getUrl(),
				importService.getSubscription().getToken()))
				.thenReturn(actualReleaseVersion);

		// TODO: Create mock response file -> "CIEL_20150614-testdata.20150622121229.tar"
		final String actualOclResponseTarfilename = "CIEL_20150614-testdata.20150622121229.tar";
		File actualOclResponseTarfile = new File("src/test/resources/", actualOclResponseTarfilename);
		FileInputStream actualFileInputStream = new FileInputStream(actualOclResponseTarfile);

		// Mock further response
		when(mockedGetMethod.getResponseBodyAsStream()).thenReturn(actualFileInputStream);
		when(mockedGetMethod.getResponseContentLength()).thenReturn(Long.valueOf(actualFileInputStream.available()));

		// Last Release Update
		importer.runTask();

		//TODO Assertion

	}

	@Ignore("WIP - OCLM-46")
	@Test
	public void mockUpdate_shouldImportMockedSnapshotResponse() throws Exception {

		Subscription subscription = new Subscription();
		subscription.setUrl("http://api.openconceptlab.com/orgs/CIEL/sources/CIEL");
		subscription.setToken("41062d8fd2bcf5eb5457988bbe2dcb5446a80a07");
		subscription.setSubscribedToSnapshot(true);

		final String filename = "CIEL_20150514-testdata-short.20150622121229.tar";

		final String releaseVersion = "1.2";

		//TODO see API for snapshot exportURL
		final String exportUrl = "https://ocl-source-export-production.s3.amazonaws.com/CIEL/"
				+ filename
				+ "?Signature=p6C4oOtz%2Fgcom5PqXDDnFexeLfg%3D&Expires=1478508163&AWSAccessKeyId=AKIAJSVYSQTANHNWOOPQ";

		//TODO: getSubscription() in ImportServiceImpl.java
		importService.saveSubscription(subscription);

		// Mock latest release version http request
		doReturn(releaseVersion).when(mockedOclClient).fetchLatestOclReleaseVersion(
				importService.getSubscription().getUrl(),
				importService.getSubscription().getToken());

		// Mock export url
		doReturn(exportUrl).when(mockedOclClient).fetchExportUrl(
				importService.getSubscription().getUrl(),
				importService.getSubscription().getToken(),
				releaseVersion);

		doReturn(mockedGetMethod).when(mockedOclClient).executeResponse(exportUrl);

		final String dateHeaderValue = "Mon, 07 Nov 2016 09:52:34 GMT";
		Header dateHeader = new Header();
		dateHeader.setName("Date");
		dateHeader.setValue(dateHeaderValue);

		// Mock response date header
		when(mockedGetMethod.getStatusCode()).thenReturn(200);
		when(mockedGetMethod.getResponseHeader("Date")).thenReturn(dateHeader);

		File oclInitialResponseTarfile = new File("src/test/resources/", filename);
		FileInputStream initialResponseFileInputStream = new FileInputStream(oclInitialResponseTarfile);

		// Mock response
		when(mockedGetMethod.getResponseBodyAsStream()).thenReturn(initialResponseFileInputStream);
		when(mockedGetMethod.getResponseContentLength()).thenReturn(Long.valueOf(initialResponseFileInputStream.available()));
		when(mockedGetMethod.getPath()).thenReturn(null);

		doReturn(new Date()).when(mockedOclClient).parseDateFromPath(null);

		//TODO Mock thread in ImportTask

		importer.setOclClient(mockedOclClient);
		importer.setImportService(importService);

		// Initial update
		importer.runTask();
		//TODO: Wait here?

		//TODO Snapshot logic mock
		//TODO Assertion

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


    public static <T> T getTargetObject(Object proxy) {
        if ((AopUtils.isJdkDynamicProxy(proxy))) {
            try {
                return (T) getTargetObject(((Advised) proxy).getTargetSource().getTarget());
            } catch (Exception e) {
                throw new RuntimeException("Failed to unproxy target.", e);
            }
        }
        return (T) proxy;
    }


}