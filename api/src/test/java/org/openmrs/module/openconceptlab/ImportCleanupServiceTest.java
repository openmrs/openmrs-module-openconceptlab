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

import org.junit.jupiter.api.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ImportCleanupServiceTest extends BaseModuleContextSensitiveTest {

	private static final long MILLIS_PER_DAY = 86400000L;

	@Autowired
	@Qualifier("openconceptlab.importService")
	private ImportService importService;

	@Test
	public void purgeOldImports_shouldPurgeImportsOlderThanRetentionPeriodWithTheirItems() throws Exception {
		Import oldImport = createFailedImport(200, 3);
		Import recentImport = createSuccessfulImport(1, 1);

		int purged = importService.purgeOldImports(90, 10);

		assertEquals(1, purged);
		assertImportPurged(oldImport.getImportId());
		assertEquals(0, importService.getImportItemsCount(oldImport, Collections.<ItemState> emptySet()).intValue());
		assertNotNull(importService.getImport(recentImport.getImportId()));
	}

	@Test
	public void purgeOldImports_shouldKeepImportsWithinRetentionPeriod() throws Exception {
		Import recentImport = createFailedImport(5, 1);

		int purged = importService.purgeOldImports(90, 10);

		assertEquals(0, purged);
		assertNotNull(importService.getImport(recentImport.getImportId()));
	}

	@Test
	public void purgeOldImports_shouldNeverPurgeLastSuccessfulSubscriptionImport() throws Exception {
		Import oldFailedImport = createFailedImport(500, 2);
		Import oldSuccessfulImport = createSuccessfulImport(400, 2);

		int purged = importService.purgeOldImports(90, 10);

		assertEquals(1, purged);
		assertImportPurged(oldFailedImport.getImportId());
		assertNotNull(importService.getImport(oldSuccessfulImport.getImportId()));
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
		Import oldestImport = createFailedImport(300, 1);
		Import olderImport = createFailedImport(200, 1);
		Import oldImport = createFailedImport(150, 1);

		int purged = importService.purgeOldImports(90, 2);

		assertEquals(2, purged);
		assertImportPurged(oldestImport.getImportId());
		assertImportPurged(olderImport.getImportId());
		assertNotNull(importService.getImport(oldImport.getImportId()));

		assertEquals(1, importService.purgeOldImports(90, 2));
		assertImportPurged(oldImport.getImportId());
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
		Import oldestImport = createFailedImport(300, 0);
		Import olderImport = createFailedImport(200, 0);
		createFailedImport(5, 0);
		Import inProgressImport = new Import();
		importService.startImport(inProgressImport);

		Date cutoff = new Date(System.currentTimeMillis() - 90 * MILLIS_PER_DAY);

		List<Import> imports = importService.getImportsStoppedBefore(cutoff, null, 10);
		assertEquals(2, imports.size());
		assertEquals(oldestImport.getImportId(), imports.get(0).getImportId());
		assertEquals(olderImport.getImportId(), imports.get(1).getImportId());

		imports = importService.getImportsStoppedBefore(cutoff, oldestImport.getImportId(), 10);
		assertEquals(1, imports.size());
		assertEquals(olderImport.getImportId(), imports.get(0).getImportId());

		imports = importService.getImportsStoppedBefore(cutoff, null, 1);
		assertEquals(1, imports.size());
		assertEquals(oldestImport.getImportId(), imports.get(0).getImportId());
	}

	private Import createSuccessfulImport(int stoppedDaysAgo, int itemCount) throws Exception {
		return createImport(true, stoppedDaysAgo, itemCount);
	}

	private Import createFailedImport(int stoppedDaysAgo, int itemCount) throws Exception {
		return createImport(false, stoppedDaysAgo, itemCount);
	}

	private Import createImport(boolean successful, int stoppedDaysAgo, int itemCount) throws Exception {
		Import anImport = new Import();
		importService.startImport(anImport);
		if (successful) {
			importService.updateOclDateStarted(anImport, new Date());
		} else {
			importService.failImport(anImport, "test failure");
		}
		for (int i = 0; i < itemCount; i++) {
			OclConcept concept = new OclConcept();
			concept.setUrl("/orgs/test/sources/test/concepts/" + UUID.randomUUID() + "/");
			concept.setVersionUrl(concept.getUrl() + "1/");
			concept.setExternalId(UUID.randomUUID().toString());
			importService.saveItem(new Item(anImport, concept, ItemState.ADDED));
		}
		importService.stopImport(anImport);
		backdateLocalDateStopped(anImport.getImportId(), stoppedDaysAgo);
		return importService.getImport(anImport.getImportId());
	}

	private void backdateLocalDateStopped(Long importId, int daysAgo) throws Exception {
		Context.flushSession();
		PreparedStatement statement = getConnection()
		        .prepareStatement("update openconceptlab_import set local_date_stopped = ? where import_id = ?");
		try {
			statement.setTimestamp(1, new Timestamp(System.currentTimeMillis() - daysAgo * MILLIS_PER_DAY));
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