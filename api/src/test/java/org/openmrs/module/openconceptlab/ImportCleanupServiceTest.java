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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.util.AopTestUtils;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ImportCleanupServiceTest extends BaseModuleContextSensitiveTest {

	private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

	@Autowired
	@Qualifier("openconceptlab.importService")
	private ImportService importService;

	@BeforeEach
	public void fixClock() {
		ImportServiceImpl impl = AopTestUtils.getUltimateTargetObject(importService);
		impl.setClock(Clock.fixed(NOW, ZoneId.systemDefault()));
	}

	@Test
	public void purgeOldImports_shouldPurgeImportsOlderThanRetentionPeriodWithSupersededItems() throws Exception {
		Import oldImport = createFailedImport(200, "/concepts/a/", "/concepts/b/");
		Import recentImport = createSuccessfulImport(1, "/concepts/a/", "/concepts/b/");

		int purged = importService.purgeOldImports(90, 10);

		assertEquals(1, purged);
		assertImportPurged(oldImport.getImportId());
		assertEquals(0, itemCount(oldImport));
		assertNotNull(importService.getImport(recentImport.getImportId()));
		assertEquals(2, itemCount(recentImport));
	}

	@Test
	public void purgeOldImports_shouldKeepImportsWithinRetentionPeriod() throws Exception {
		Import recentImport = createFailedImport(5, "/concepts/a/");

		int purged = importService.purgeOldImports(90, 10);

		assertEquals(0, purged);
		assertNotNull(importService.getImport(recentImport.getImportId()));
	}

	@Test
	public void purgeOldImports_shouldNeverPurgeLastSuccessfulSubscriptionImport() throws Exception {
		Import oldFailedImport = createFailedImport(500, "/concepts/a/");
		Import oldSuccessfulImport = createSuccessfulImport(400, "/concepts/a/");

		int purged = importService.purgeOldImports(90, 10);

		assertEquals(1, purged);
		assertImportPurged(oldFailedImport.getImportId());
		assertNotNull(importService.getImport(oldSuccessfulImport.getImportId()));
		assertEquals(1, itemCount(oldSuccessfulImport));
	}

	@Test
	public void purgeOldImports_shouldKeepLastSuccessfulSubscriptionImportEvenIfAllItsItemsAreSuperseded() throws Exception {
		Import oldSuccessfulImport = createSuccessfulImport(400, "/concepts/a/");
		createFailedImport(200, "/concepts/a/");

		int purged = importService.purgeOldImports(90, 10);

		assertEquals(0, purged);
		assertNotNull(importService.getImport(oldSuccessfulImport.getImportId()));
		assertEquals(1, itemCount(oldSuccessfulImport));
	}

	@Test
	public void purgeOldImports_shouldPagePastRetainedImportsToReachPurgeableOnes() throws Exception {
		createFailedImport(400, "/concepts/only-a/");
		createFailedImport(390, "/concepts/only-b/");
		Import superseded = createFailedImport(300, "/concepts/x/");
		createSuccessfulImport(1, "/concepts/x/");

		int purged = importService.purgeOldImports(90, 2);

		assertEquals(1, purged);
		assertImportPurged(superseded.getImportId());
	}

	@Test
	public void purgeOldImports_shouldReachPurgeableImportBehindMultiplePagesOfRetainedImports() throws Exception {
		createFailedImport(400, "/concepts/only-a/");
		createFailedImport(390, "/concepts/only-b/");
		createFailedImport(380, "/concepts/only-c/");
		createFailedImport(370, "/concepts/only-d/");
		createFailedImport(360, "/concepts/only-e/");
		Import superseded = createFailedImport(300, "/concepts/x/");
		createSuccessfulImport(1, "/concepts/x/");

		int purged = importService.purgeOldImports(90, 2);

		assertEquals(1, purged);
		assertImportPurged(superseded.getImportId());
	}

	@Test
	public void purgeOldImports_shouldNotSkipPurgeableImportsInterleavedWithRetainedOnes() throws Exception {
		Import purgeableA = createFailedImport(400);
		createFailedImport(390, "/concepts/only-a/");
		Import purgeableB = createFailedImport(380);
		createFailedImport(370, "/concepts/only-b/");

		int purged = importService.purgeOldImports(90, 2);

		assertEquals(2, purged);
		assertImportPurged(purgeableA.getImportId());
		assertImportPurged(purgeableB.getImportId());
	}

	@Test
	public void purgeOldImports_shouldKeepInProgressImports() throws Exception {
		Import inProgressImport = new Import();
		importService.startImport(inProgressImport);

		int purged = importService.purgeOldImports(90, 10);

		assertEquals(0, purged);
		assertNotNull(importService.getImport(inProgressImport.getImportId()));
	}

	@Test
	public void purgeOldImports_shouldPurgeNoMoreThanBatchSizeImportsOldestFirst() throws Exception {
		Import oldestImport = createFailedImport(300);
		Import olderImport = createFailedImport(200);
		Import oldImport = createFailedImport(150);

		int purged = importService.purgeOldImports(90, 2);

		assertEquals(2, purged);
		assertImportPurged(oldestImport.getImportId());
		assertImportPurged(olderImport.getImportId());
		assertNotNull(importService.getImport(oldImport.getImportId()));

		assertEquals(1, importService.purgeOldImports(90, 2));
		assertImportPurged(oldImport.getImportId());
	}

	@Test
	public void purgeOldImports_shouldKeepItemsThatAreTheLastUsableRecordForTheirUrl() throws Exception {
		Import oldImport = createFailedImport(200, "/concepts/only-record/");

		int purged = importService.purgeOldImports(90, 10);

		assertEquals(0, purged);
		assertNotNull(importService.getImport(oldImport.getImportId()));
		assertEquals(1, itemCount(oldImport));
	}

	@Test
	public void purgeOldImports_shouldTrimSupersededItemsButKeepImportWithLastUsableRecords() throws Exception {
		Import oldImport = createFailedImport(200, "/concepts/superseded/", "/concepts/only-record/");
		Import recentImport = createSuccessfulImport(1, "/concepts/superseded/");

		int purged = importService.purgeOldImports(90, 10);

		assertEquals(0, purged);
		assertNotNull(importService.getImport(oldImport.getImportId()));
		List<Item> remainingItems = importService.getImportItems(oldImport, 0, 10, Collections.<ItemState> emptySet());
		assertEquals(1, remainingItems.size());
		assertEquals("/concepts/only-record/", remainingItems.get(0).getUrl());
		assertEquals(1, itemCount(recentImport));
	}

	@Test
	public void purgeOldImports_shouldAlwaysDeleteErrorItems() throws Exception {
		Import oldImport = createFailedImport(200);
		addItem(oldImport, "/concepts/failed-item/", ItemState.ERROR);

		int purged = importService.purgeOldImports(90, 10);

		assertEquals(1, purged);
		assertImportPurged(oldImport.getImportId());
	}

	@Test
	public void purgeOldImports_shouldSkipImportThatFailsToPurgeAndContinueWithTheRest() throws Exception {
		Import failingImport = createFailedImport(300);
		Import purgeableImport = createFailedImport(200);

		ImportService realService = importService;
		ImportService throwingService = Mockito.mock(ImportService.class, AdditionalAnswers.delegatesTo(realService));
		Mockito.doThrow(new RuntimeException("simulated purge failure")).when(throwingService)
		        .purgeImport(failingImport.getImportId());
		ServiceContext.getInstance().setService(ImportService.class, throwingService);
		try {
			int purged = importService.purgeOldImports(90, 10);

			assertEquals(1, purged);
			assertNotNull(importService.getImport(failingImport.getImportId()));
			assertImportPurged(purgeableImport.getImportId());
		}
		finally {
			ServiceContext.getInstance().setService(ImportService.class, realService);
		}
	}

	@Test
	public void purgeOldImports_shouldThrowIfRetentionDaysIsNotPositive() {
		assertThrows(IllegalArgumentException.class, () -> importService.purgeOldImports(0, 10));
	}

	@Test
	public void purgeOldImports_shouldThrowIfBatchSizeIsNotPositive() {
		assertThrows(IllegalArgumentException.class, () -> importService.purgeOldImports(90, 0));
	}

	@Test
	public void getImportsStoppedBefore_shouldReturnImportsStoppedBeforeCutoffOldestFirstExcludingGivenImport()
	        throws Exception {
		Import oldestImport = createFailedImport(300);
		Import olderImport = createFailedImport(200);
		createFailedImport(5);
		Import inProgressImport = new Import();
		importService.startImport(inProgressImport);

		Date cutoff = Date.from(NOW.minus(90, ChronoUnit.DAYS));

		List<Import> imports = importService.getImportsStoppedBefore(cutoff, null, 0, 10);
		assertEquals(2, imports.size());
		assertEquals(oldestImport.getImportId(), imports.get(0).getImportId());
		assertEquals(olderImport.getImportId(), imports.get(1).getImportId());

		imports = importService.getImportsStoppedBefore(cutoff, oldestImport.getImportId(), 0, 10);
		assertEquals(1, imports.size());
		assertEquals(olderImport.getImportId(), imports.get(0).getImportId());

		imports = importService.getImportsStoppedBefore(cutoff, null, 0, 1);
		assertEquals(1, imports.size());
		assertEquals(oldestImport.getImportId(), imports.get(0).getImportId());

		imports = importService.getImportsStoppedBefore(cutoff, null, 1, 10);
		assertEquals(1, imports.size());
		assertEquals(olderImport.getImportId(), imports.get(0).getImportId());
	}

	private Import createSuccessfulImport(int stoppedDaysAgo, String... itemUrls) throws Exception {
		return createImport(true, stoppedDaysAgo, itemUrls);
	}

	private Import createFailedImport(int stoppedDaysAgo, String... itemUrls) throws Exception {
		return createImport(false, stoppedDaysAgo, itemUrls);
	}

	private Import createImport(boolean successful, int stoppedDaysAgo, String... itemUrls) throws Exception {
		Import anImport = new Import();
		importService.startImport(anImport);
		if (successful) {
			importService.updateOclDateStarted(anImport, Date.from(NOW));
		} else {
			importService.failImport(anImport, "test failure");
		}
		for (String url : itemUrls) {
			addItem(anImport, url, ItemState.ADDED);
		}
		importService.stopImport(anImport);
		backdateLocalDateStopped(anImport.getImportId(), stoppedDaysAgo);
		return importService.getImport(anImport.getImportId());
	}

	private void addItem(Import anImport, String url, ItemState state) {
		OclConcept concept = new OclConcept();
		concept.setUrl(url);
		concept.setVersionUrl(url + "1/");
		concept.setExternalId(UUID.randomUUID().toString());
		importService.saveItem(new Item(anImport, concept, state));
	}

	private int itemCount(Import anImport) {
		return importService.getImportItemsCount(anImport, Collections.<ItemState> emptySet());
	}

	private void backdateLocalDateStopped(Long importId, int daysAgo) throws Exception {
		Context.flushSession();
		PreparedStatement statement = getConnection()
		        .prepareStatement("update openconceptlab_import set local_date_stopped = ? where import_id = ?");
		try {
			statement.setTimestamp(1, Timestamp.from(NOW.minus(daysAgo, ChronoUnit.DAYS)));
			statement.setLong(2, importId);
			statement.executeUpdate();
		}
		finally {
			statement.close();
		}
		Context.clearSession();
	}

	private void assertImportPurged(Long importId) {
		assertThrows(IllegalArgumentException.class, () -> importService.getImport(importId),
		    "Import " + importId + " should have been purged");
	}
}
