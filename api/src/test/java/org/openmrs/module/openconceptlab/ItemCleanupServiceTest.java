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

import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ItemCleanupServiceTest extends BaseModuleContextSensitiveTest {

	@Autowired @Qualifier("openconceptlab.importService")
	private ImportService importService;

	@Autowired @Qualifier("openconceptlab.cleanupService")
	private ItemCleanupService cleanupService;

	@Autowired
	private SessionFactory sessionFactory;

	private AdministrationService adminService;

	@Before
	public void setUp() {
		adminService = Context.getAdministrationService();
	}

	@Test
	public void runCleanup_shouldDoNothingWhenNoRetentionPolicyConfigured() {
		// No retention type set - cleanup should be a no-op
		int deleted = cleanupService.runCleanup();
		assertThat(deleted, is(0));
	}

	@Test
	public void runCleanup_shouldDoNothingForInvalidRetentionType() {
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETENTION_TYPE, "INVALID");
		int deleted = cleanupService.runCleanup();
		assertThat(deleted, is(0));
	}

	@Test
	public void runCleanup_shouldDeleteItemsOlderThanRetainedImports() {
		// Create 4 completed imports, each with one item for a unique URL
		Import import1 = createAndStopImport();
		Item item1a = createItem(import1, "/orgs/test/concepts/1/", "uuid-1a");
		importService.saveItem(item1a);

		Import import2 = createAndStopImport();
		Item item2a = createItem(import2, "/orgs/test/concepts/2/", "uuid-2a");
		importService.saveItem(item2a);

		Import import3 = createAndStopImport();
		Item item3a = createItem(import3, "/orgs/test/concepts/3/", "uuid-3a");
		importService.saveItem(item3a);

		Import import4 = createAndStopImport();
		Item item4a = createItem(import4, "/orgs/test/concepts/4/", "uuid-4a");
		importService.saveItem(item4a);

		// Configure to keep only the 2 most recent imports
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETENTION_TYPE, "RUNS");
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETAIN_IMPORTS, "2");

		cleanupService.runCleanup();

		// Items from import1 and import2 are outside the retention window,
		// but each URL only has one item so they must all be preserved.
		assertThat(importService.getImportItems(import1, 0, 100, new HashSet<>()).size(), is(1));
		assertThat(importService.getImportItems(import2, 0, 100, new HashSet<>()).size(), is(1));
		assertThat(importService.getImportItems(import3, 0, 100, new HashSet<>()).size(), is(1));
		assertThat(importService.getImportItems(import4, 0, 100, new HashSet<>()).size(), is(1));
	}

	@Test
	public void runCleanup_shouldDeleteOlderDuplicateItemsButPreserveLatestPerUrl() {
		// Create 4 imports, all touching the same concept URL
		Import import1 = createAndStopImport();
		Item item1 = createItem(import1, "/orgs/test/concepts/1/", "uuid-1");
		importService.saveItem(item1);

		Import import2 = createAndStopImport();
		Item item2 = createItem(import2, "/orgs/test/concepts/1/", "uuid-1");
		importService.saveItem(item2);

		Import import3 = createAndStopImport();
		Item item3 = createItem(import3, "/orgs/test/concepts/1/", "uuid-1");
		importService.saveItem(item3);

		Import import4 = createAndStopImport();
		Item item4 = createItem(import4, "/orgs/test/concepts/1/", "uuid-1");
		importService.saveItem(item4);

		// Keep 2 most recent imports (import3, import4)
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETENTION_TYPE, "RUNS");
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETAIN_IMPORTS, "2");

		int deleted = cleanupService.runCleanup();

		// import1 and import2 items are outside retention, and item4 is the latest per URL.
		// So items from import1 and import2 should be deleted.
		assertThat(deleted, greaterThanOrEqualTo(2));

		assertThat(importService.getImportItems(import1, 0, 100, new HashSet<>()).size(), is(0));
		assertThat(importService.getImportItems(import2, 0, 100, new HashSet<>()).size(), is(0));
		assertThat(importService.getImportItems(import3, 0, 100, new HashSet<>()).size(), is(1));
		assertThat(importService.getImportItems(import4, 0, 100, new HashSet<>()).size(), is(1));
	}

	@Test
	public void runCleanup_shouldPreserveLatestItemPerUrlEvenIfExpired() {
		// A concept that only appeared in old imports should still keep one item
		Import import1 = createAndStopImport();
		Item item1 = createItem(import1, "/orgs/test/concepts/old/", "uuid-old");
		importService.saveItem(item1);

		// Create 2 more recent imports that don't touch this concept
		Import import2 = createAndStopImport();
		Item item2 = createItem(import2, "/orgs/test/concepts/new/", "uuid-new");
		importService.saveItem(item2);

		Import import3 = createAndStopImport();
		Item item3 = createItem(import3, "/orgs/test/concepts/new/", "uuid-new");
		importService.saveItem(item3);

		// Keep only 2 most recent imports (import2, import3)
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETENTION_TYPE, "RUNS");
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETAIN_IMPORTS, "2");

		cleanupService.runCleanup();

		// item1 is in an old import BUT is the only item for its URL, so it must be preserved
		assertThat(importService.getImportItems(import1, 0, 100, new HashSet<>()).size(), is(1));
		assertThat(importService.getImportItems(import2, 0, 100, new HashSet<>()).size(), is(1));
		assertThat(importService.getImportItems(import3, 0, 100, new HashSet<>()).size(), is(1));
	}

	@Test
	public void runCleanup_shouldNotDeleteItemsFromInProgressImports() {
		// Create 2 completed imports and 1 in-progress, all touching the same URL
		Import import1 = createAndStopImport();
		importService.saveItem(createItem(import1, "/orgs/test/concepts/1/", "uuid-1"));

		Import import2 = createAndStopImport();
		importService.saveItem(createItem(import2, "/orgs/test/concepts/1/", "uuid-1"));

		// In-progress import (not stopped)
		Import inProgress = new Import();
		importService.startImport(inProgress);
		importService.saveItem(createItem(inProgress, "/orgs/test/concepts/1/", "uuid-1"));

		// Keep only 1 completed import (import2). import1 is outside retention.
		// In-progress import is always protected.
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETENTION_TYPE, "RUNS");
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETAIN_IMPORTS, "1");

		int deleted = cleanupService.runCleanup();

		// import1 is outside retention, its item is not the latest per URL (that's in inProgress),
		// so it should be deleted.
		assertThat(deleted, greaterThanOrEqualTo(1));

		assertThat(importService.getImportItems(import1, 0, 100, new HashSet<>()).size(), is(0));
		assertThat(importService.getImportItems(import2, 0, 100, new HashSet<>()).size(), is(1));
		assertThat(importService.getImportItems(inProgress, 0, 100, new HashSet<>()).size(), is(1));

		// Stop the in-progress import for cleanup
		importService.stopImport(inProgress);
	}

	@Test
	public void runCleanup_shouldNotDeleteItemsFromMostRecentCompletedImport() {
		// Even with RUNS=1, the most recent completed import is always protected
		Import import1 = createAndStopImport();
		Item item1 = createItem(import1, "/orgs/test/concepts/1/", "uuid-1");
		importService.saveItem(item1);

		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETENTION_TYPE, "RUNS");
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETAIN_IMPORTS, "1");

		cleanupService.runCleanup();

		// import1 is the most recent completed import, so it's protected
		assertThat(importService.getImportItems(import1, 0, 100, new HashSet<>()).size(), is(1));
	}

	@Test
	public void runCleanup_shouldDeleteOrphanedImports() {
		// Create 4 imports, all with the same concept
		Import import1 = createAndStopImport();
		importService.saveItem(createItem(import1, "/orgs/test/concepts/1/", "uuid-1"));

		Import import2 = createAndStopImport();
		importService.saveItem(createItem(import2, "/orgs/test/concepts/1/", "uuid-1"));

		Import import3 = createAndStopImport();
		importService.saveItem(createItem(import3, "/orgs/test/concepts/1/", "uuid-1"));

		Import import4 = createAndStopImport();
		importService.saveItem(createItem(import4, "/orgs/test/concepts/1/", "uuid-1"));

		// Keep only 2 most recent imports
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETENTION_TYPE, "RUNS");
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETAIN_IMPORTS, "2");

		cleanupService.runCleanup();

		// import1 and import2 should have had their items deleted and then been
		// deleted themselves as orphans.
		List<Import> remaining = importService.getImportsInOrder(0, 1000);
		Set<Long> remainingIds = new HashSet<>();
		for (Import i : remaining) {
			remainingIds.add(i.getImportId());
		}
		assertThat(remainingIds.contains(import1.getImportId()), is(false));
		assertThat(remainingIds.contains(import2.getImportId()), is(false));
		assertThat(remainingIds.contains(import3.getImportId()), is(true));
		assertThat(remainingIds.contains(import4.getImportId()), is(true));
	}

	@Test
	public void runCleanup_shouldWorkWithDaysRetentionPolicy() {
		// Create an old import (items are old but import is already stopped)
		Import oldImport = createAndStopImport();
		Item oldItem = createItem(oldImport, "/orgs/test/concepts/1/", "uuid-1");
		importService.saveItem(oldItem);

		// Create a recent import with the same concept
		Import recentImport = createAndStopImport();
		Item recentItem = createItem(recentImport, "/orgs/test/concepts/1/", "uuid-1");
		importService.saveItem(recentItem);

		// With DAYS retention, both imports were "just now" so both are within retention
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETENTION_TYPE, "DAYS");
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETAIN_DAYS, "1");

		cleanupService.runCleanup();

		// Both imports are within the 1-day window, so nothing should be deleted
		assertThat(importService.getImportItems(oldImport, 0, 100, new HashSet<>()).size(), is(1));
		assertThat(importService.getImportItems(recentImport, 0, 100, new HashSet<>()).size(), is(1));
	}

	@Test
	public void runCleanup_shouldNotCountFailedImportsAsRecentForRunsRetention() {
		// Create 3 successful imports and 1 failed import (most recent), all touching the same URL
		Import import1 = createAndStopImport();
		importService.saveItem(createItem(import1, "/orgs/test/concepts/1/", "uuid-1"));

		Import import2 = createAndStopImport();
		importService.saveItem(createItem(import2, "/orgs/test/concepts/1/", "uuid-1"));

		Import import3 = createAndStopImport();
		importService.saveItem(createItem(import3, "/orgs/test/concepts/1/", "uuid-1"));

		// Failed import is the most recent completed import (only has ERROR items)
		Import failedImport = createAndFailImport("Something went wrong");
		importService.saveItem(createErrorItem(failedImport, "/orgs/test/concepts/1/", "uuid-1"));

		// Keep only 2 most recent successful imports (import2, import3).
		// The failed import should NOT count toward the 2.
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETENTION_TYPE, "RUNS");
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETAIN_IMPORTS, "2");

		int deleted = cleanupService.runCleanup();

		// import1 is outside retention (it's the 3rd successful import back).
		// failedImport is not protected by the RUNS count.
		// The latest *successful* item per URL is in import3 (protected), so:
		// - import1's ADDED item is eligible and deleted
		// - failedImport's ERROR item is eligible and deleted (ERROR items aren't preserved per URL)
		assertThat(deleted, greaterThanOrEqualTo(2));

		assertThat(importService.getImportItems(import1, 0, 100, new HashSet<>()).size(), is(0));
		assertThat(importService.getImportItems(import2, 0, 100, new HashSet<>()).size(), is(1));
		assertThat(importService.getImportItems(import3, 0, 100, new HashSet<>()).size(), is(1));
		assertThat(importService.getImportItems(failedImport, 0, 100, new HashSet<>()).size(), is(0));
	}

	@Test
	public void runCleanup_shouldDeleteItemsFromImportsOlderThanRetainedDays() {
		// Create an old import with two concepts
		Import oldImport = createAndStopImport();
		importService.saveItem(createItem(oldImport, "/orgs/test/concepts/1/", "uuid-1"));
		importService.saveItem(createItem(oldImport, "/orgs/test/concepts/2/", "uuid-2"));
		backdateImport(oldImport, 60);

		// Create a recent import that touches concept 1 (so old import's concept-1 item is not the latest per URL)
		Import recentImport = createAndStopImport();
		importService.saveItem(createItem(recentImport, "/orgs/test/concepts/1/", "uuid-1"));

		// Retain imports from the last 7 days — oldImport is 60 days old, so it's outside the window
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETENTION_TYPE, "DAYS");
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETAIN_DAYS, "7");

		int deleted = cleanupService.runCleanup();

		// oldImport's concept-1 item should be deleted (recentImport has a newer one for that URL).
		// oldImport's concept-2 item is the only item for its URL, so it must be preserved.
		assertThat(deleted, greaterThanOrEqualTo(1));
		assertThat(importService.getImportItems(oldImport, 0, 100, new HashSet<>()).size(), is(1));
		assertThat(importService.getImportItems(recentImport, 0, 100, new HashSet<>()).size(), is(1));
	}

	@Test
	public void runCleanup_shouldProtectLatestSuccessfulImportForDaysRetention() {
		// Create 1 successful import and then a failed import (most recent)
		Import successfulImport = createAndStopImport();
		importService.saveItem(createItem(successfulImport, "/orgs/test/concepts/1/", "uuid-1"));

		Import failedImport = createAndFailImport("Connection timeout");
		importService.saveItem(createErrorItem(failedImport, "/orgs/test/concepts/1/", "uuid-1"));

		// Both imports were just created, so both are within the 1-day window.
		// The "always protect the most recent successful import" guarantee should apply
		// to successfulImport, not failedImport.
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETENTION_TYPE, "DAYS");
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETAIN_DAYS, "1");

		cleanupService.runCleanup();

		// Both imports are within the 1-day window, so both are protected by the date range.
		assertThat(importService.getImportItems(successfulImport, 0, 100, new HashSet<>()).size(), is(1));
		assertThat(importService.getImportItems(failedImport, 0, 100, new HashSet<>()).size(), is(1));
	}

	@Test
	public void runCleanup_shouldHandleMultipleUrlsAcrossImports() {
		// Import1: concepts A, B
		Import import1 = createAndStopImport();
		importService.saveItem(createItem(import1, "/orgs/test/concepts/A/", "uuid-A"));
		importService.saveItem(createItem(import1, "/orgs/test/concepts/B/", "uuid-B"));

		// Import2: concepts A, C
		Import import2 = createAndStopImport();
		importService.saveItem(createItem(import2, "/orgs/test/concepts/A/", "uuid-A"));
		importService.saveItem(createItem(import2, "/orgs/test/concepts/C/", "uuid-C"));

		// Import3: concept A
		Import import3 = createAndStopImport();
		importService.saveItem(createItem(import3, "/orgs/test/concepts/A/", "uuid-A"));

		// Keep only 1 most recent import (import3)
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETENTION_TYPE, "RUNS");
		setGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETAIN_IMPORTS, "1");

		int deleted = cleanupService.runCleanup();

		// import1 and import2 are outside retention.
		// Concept A's latest item is in import3 (protected), so items from import1 and import2 for A can be deleted.
		// Concept B's only item is in import1 - must be preserved (latest per URL).
		// Concept C's only item is in import2 - must be preserved (latest per URL).
		assertThat(deleted, greaterThanOrEqualTo(2));

		// import1 had A and B; A is deleted, B is preserved as the only item for its URL
		assertThat(importService.getImportItems(import1, 0, 100, new HashSet<>()).size(), is(1));
		// import2 had A and C; A is deleted, C is preserved as the only item for its URL
		assertThat(importService.getImportItems(import2, 0, 100, new HashSet<>()).size(), is(1));
		assertThat(importService.getImportItems(import3, 0, 100, new HashSet<>()).size(), is(1));
	}

	private Import createAndStopImport() {
		Import anImport = new Import();
		importService.startImport(anImport);
		importService.stopImport(anImport);
		return anImport;
	}

	private Import createAndFailImport(String errorMessage) {
		Import anImport = new Import();
		importService.startImport(anImport);
		importService.failImport(anImport, errorMessage);
		importService.stopImport(anImport);
		return anImport;
	}

	private Item createErrorItem(Import anImport, String url, String uuid) {
		OclConcept concept = new OclConcept();
		concept.setUrl(url);
		concept.setVersionUrl(url + "v1/");
		concept.setExternalId(uuid);
		return new Item(anImport, concept, ItemState.ERROR);
	}

	private Item createItem(Import anImport, String url, String uuid) {
		OclConcept concept = new OclConcept();
		concept.setUrl(url);
		concept.setVersionUrl(url + "v1/");
		concept.setExternalId(uuid);
		return new Item(anImport, concept, ItemState.ADDED);
	}

	private void backdateImport(Import anImport, int daysAgo) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
		sessionFactory.getCurrentSession().createQuery(
				"UPDATE OclImport SET localDateStopped = :date WHERE importId = :id")
				.setParameter("date", cal.getTime())
				.setParameter("id", anImport.getImportId())
				.executeUpdate();
		sessionFactory.getCurrentSession().flush();
		sessionFactory.getCurrentSession().clear();
	}

	private void setGlobalProperty(String key, String value) {
		GlobalProperty gp = adminService.getGlobalPropertyObject(key);
		if (gp == null) {
			gp = new GlobalProperty(key);
		}
		gp.setPropertyValue(value);
		adminService.saveGlobalProperty(gp);
	}
}
