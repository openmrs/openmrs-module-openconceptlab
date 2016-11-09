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

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.openconceptlab.CacheService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.TestResources;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportServiceImpl;
import org.openmrs.module.openconceptlab.client.OclClient;
import org.openmrs.module.openconceptlab.client.OclClient.OclResponse;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclMapping;
import org.openmrs.test.BaseContextMockTest;

public class ImporterTest extends BaseContextMockTest {

	@Mock
	OclClient oclClient;

	@Mock
	ContextDAO contextDAO;

	@Mock
	ImportServiceImpl importService;

	@Mock
	CacheService CacheService;

	@Mock
	Saver saver;

    @Mock
    Subscription subscription;

	@InjectMocks
	Importer importer;

	@Before
	public void before() {
		TestResources.setupDaemonToken();
	}

	/**
	 * @see Importer#run()
	 * @verifies start first anImport with response date
	 */
	@Test
	public void runUpdate_shouldStartFirstUpdateWithResponseDate() throws Exception {
		Subscription subscription = new Subscription();
		subscription.setUrl("http://some.com/url");
		when(importService.getSubscription()).thenReturn(subscription);

		Date updatedTo = new Date();
		OclResponse oclResponse = new OclClient.OclResponse(IOUtils.toInputStream("{}"), 0, updatedTo);
		when(importService.getLastImport()).thenReturn(null);
		when(oclClient.fetchLastReleaseVersion(subscription.getUrl(), subscription.getToken())).thenReturn(oclResponse);

		importer.run();

		verify(importService).updateOclDateStarted(Mockito.any(Import.class), Mockito.eq(updatedTo));
	}

	/**
	 * @see Importer#run()
	 * @verifies start further RELEASE update
	 */
	@Test
	public void runUpdate_shouldStartUpdateIfNewRelease() throws Exception {

        final String release1name = "1.0";
        final String release2name = "1.1";

        subscription.setUrl("http://some.com/url");
		when(importService.getSubscription()).thenReturn(subscription);
		when(subscription.isSubscribedToSnapshot()).thenReturn(false);

		Import lastImport = new Import();
		Date updatedSince = new Date();
		lastImport.setOclDateStarted(updatedSince);
		lastImport.setReleaseVersion(release1name);

		when(importService.getLastSuccessfulSubscriptionImport()).thenReturn(lastImport);

        OclResponse oclResponse = new OclClient.OclResponse(IOUtils.toInputStream("{}"), 0, new Date());

        when(oclClient.fetchLatestOclReleaseVersion(subscription.getUrl(), subscription.getToken())).thenReturn(release2name);
        when(oclClient.fetchLastReleaseVersion(subscription.getUrl(), subscription.getToken(), lastImport.getReleaseVersion())).thenReturn(oclResponse);

        importer.run();
    }

	/**
	 * @see Importer#run()
	 * @verifies start further SNAPSHOT import with updated since
	 */
	@Test
	public void runUpdate_shouldStartNextUpdateWithUpdatedSince() throws Exception {
		subscription.setUrl("http://some.com/url");
		when(importService.getSubscription()).thenReturn(subscription);
		when(subscription.isSubscribedToSnapshot()).thenReturn(true);

		Import lastUpdate = new Import();
		Date updatedSince = new Date();
		lastUpdate.setOclDateStarted(updatedSince);
		when(importService.getLastSuccessfulSubscriptionImport()).thenReturn(lastUpdate);

		Date updatedTo = new Date();
		OclResponse oclResponse = new OclClient.OclResponse(IOUtils.toInputStream("{}"), 0, updatedTo);
		when(oclClient.fetchSnapshotUpdates(subscription.getUrl(), subscription.getToken(), lastUpdate.getOclDateStarted()))
		        .thenReturn(oclResponse);

		importer.run();

		verify(importService).updateOclDateStarted(Mockito.any(Import.class), Mockito.eq(updatedTo));
	}

	/**
	 * @see Importer#run()
	 * @verifies create item for each concept and mapping
	 */
	@Test
	public void runUpdate_shouldCreateItemForEachConceptAndMapping() throws Exception {
		subscription.setUrl("http://some.com/url");
		when(importService.getSubscription()).thenReturn(subscription);
		when(subscription.isSubscribedToSnapshot()).thenReturn(true);

		Import lastUpdate = new Import();
		Date updatedSince = new Date();
		lastUpdate.setOclDateStarted(updatedSince);

		when(importService.getLastSuccessfulSubscriptionImport()).thenReturn(lastUpdate);

		Date updatedTo = new Date();
		OclResponse oclResponse = new OclClient().unzipResponse(TestResources.getSimpleResponseAsStream(), updatedTo);

		when(oclClient.fetchSnapshotUpdates(subscription.getUrl(), subscription.getToken(), lastUpdate.getOclDateStarted()))
		        .thenReturn(oclResponse);

		doAnswer(new Answer<Item>() {

			@Override
			public Item answer(InvocationOnMock invocation) throws Throwable {
				Import update = (Import) invocation.getArguments()[1];
				OclConcept oclConcept = (OclConcept) invocation.getArguments()[2];
				return new Item(update, oclConcept, ItemState.ADDED);
			}
		}).when(saver).saveConcept(any(CacheService.class), any(Import.class), any(OclConcept.class));

		doAnswer(new Answer<Item>() {

			@Override
			public Item answer(InvocationOnMock invocation) throws Throwable {
				Import update = (Import) invocation.getArguments()[1];
				OclMapping oclMapping = (OclMapping) invocation.getArguments()[2];
				return new Item(update, oclMapping, ItemState.ADDED);
			}

		}).when(saver).saveMapping(any(CacheService.class), any(Import.class), any(OclMapping.class));

		importer.run();

		//concepts
		verify(importService).saveItems(
		    argThat(hasItems(hasUuid("1001AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"),
		        hasUuid("1002AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"), hasUuid("1003AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))));

		//mappings
		verify(importService).saveItems(
		    argThat(hasItems(hasUuid("697bf112-a7ca-3ae3-af4f-8b46e3af7f10"),
		        hasUuid("def16c32-0635-3afd-8a56-a080830e2bff"), hasUuid("b705416c-ad04-356f-9d43-8945ee382722"))));
	}

	public Matcher<Import> hasOclDateStarted(Date oclDateStarted) {
		return new FeatureMatcher<Import, Date>(
		                                        is(oclDateStarted), "oclDateStarted", "oclDateStarted") {

			@Override
			protected Date featureValueOf(Import actual) {
				return actual.getOclDateStarted();
			}
		};
	}

	public Matcher<Item> hasUuid(String uuid) {
		return new FeatureMatcher<Item, String>(
		                                        is(uuid), "uuid", "uuid") {

			@Override
			protected String featureValueOf(Item actual) {
				return actual.getUuid();
			}
		};
	}
}
