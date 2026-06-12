/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.ImportServiceImpl;
import org.openmrs.module.openconceptlab.OpenConceptLabConstants;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ImportCleanupTaskTest extends BaseModuleContextSensitiveTest {

	private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

	@Autowired
	@Qualifier("openconceptlab.importService")
	private ImportService importService;

	@BeforeEach
	public void fixClock() throws Exception {
		ImportServiceImpl impl = (ImportServiceImpl) ((Advised) importService).getTargetSource().getTarget();
		impl.setClock(Clock.fixed(NOW, ZoneId.systemDefault()));
	}

	@Test
	public void execute_shouldDoNothingWhenCleanupIsDisabled() throws Exception {
		Import oldImport = createFailedImport(400);

		new ImportCleanupTask().execute();

		assertNotNull(importService.getImport(oldImport.getImportId()));
	}

	@Test
	public void execute_shouldPurgeOldImportsWhenEnabled() throws Exception {
		Import oldImport = createFailedImport(400);
		Import recentImport = createFailedImport(5);
		setGlobalProperty(OpenConceptLabConstants.GP_IMPORT_CLEANUP_ENABLED, "true");
		setGlobalProperty(OpenConceptLabConstants.GP_IMPORT_RETENTION_DAYS, "90");
		setGlobalProperty(OpenConceptLabConstants.GP_IMPORT_CLEANUP_BATCH_SIZE, "10");

		new ImportCleanupTask().execute();

		assertImportPurged(oldImport.getImportId());
		assertNotNull(importService.getImport(recentImport.getImportId()));
	}

	@Test
	public void execute_shouldFallBackToDefaultsOnInvalidGlobalProperties() throws Exception {
		Import oldImport = createFailedImport(ImportCleanupTask.DEFAULT_IMPORT_RETENTION_DAYS + 20);
		Import recentImport = createFailedImport(ImportCleanupTask.DEFAULT_IMPORT_RETENTION_DAYS - 20);
		setGlobalProperty(OpenConceptLabConstants.GP_IMPORT_CLEANUP_ENABLED, "true");
		setGlobalProperty(OpenConceptLabConstants.GP_IMPORT_RETENTION_DAYS, "abc");
		setGlobalProperty(OpenConceptLabConstants.GP_IMPORT_CLEANUP_BATCH_SIZE, "-5");

		assertDoesNotThrow(() -> new ImportCleanupTask().execute());

		assertImportPurged(oldImport.getImportId());
		assertNotNull(importService.getImport(recentImport.getImportId()));
	}

	private void setGlobalProperty(String property, String value) {
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(property, value));
	}

	private Import createFailedImport(int stoppedDaysAgo) throws Exception {
		Import anImport = new Import();
		importService.startImport(anImport);
		importService.failImport(anImport, "test failure");
		importService.stopImport(anImport);

		Context.flushSession();
		PreparedStatement statement = getConnection()
		        .prepareStatement("update openconceptlab_import set local_date_stopped = ? where import_id = ?");
		try {
			statement.setTimestamp(1, Timestamp.from(NOW.minus(stoppedDaysAgo, ChronoUnit.DAYS)));
			statement.setLong(2, anImport.getImportId());
			statement.executeUpdate();
		}
		finally {
			statement.close();
		}
		Context.clearSession();
		return importService.getImport(anImport.getImportId());
	}

	private void assertImportPurged(Long importId) {
		assertThrows(IllegalArgumentException.class, () -> importService.getImport(importId),
		    "Import " + importId + " should have been purged");
	}
}