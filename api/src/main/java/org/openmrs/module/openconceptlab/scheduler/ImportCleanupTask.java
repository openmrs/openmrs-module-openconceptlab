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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.OpenConceptLabConstants;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportCleanupTask extends AbstractTask {

	public static final int DEFAULT_IMPORT_RETENTION_DAYS = 180;

	public static final int DEFAULT_IMPORT_CLEANUP_BATCH_SIZE = 10;

	private static final Logger logger = LoggerFactory.getLogger(ImportCleanupTask.class);

	@Override
	public void execute() {
		try {
			cleanupImport();
		}
		catch (Exception e) {
			logger.error("OCL import cleanup failed", e);
		}
	}

	private void cleanupImport() {
		boolean isCleanupEnabled = BooleanUtils.toBoolean(Context.getAdministrationService().getGlobalProperty(OpenConceptLabConstants.GP_IMPORT_CLEANUP_ENABLED));
		if (!isCleanupEnabled) {
			logger.info("OCL import cleanup is disabled ({} is not 'true'), skipping",
			        OpenConceptLabConstants.GP_IMPORT_CLEANUP_ENABLED);
			return;
		}

		int retentionDays = getPositiveIntGP(OpenConceptLabConstants.GP_IMPORT_RETENTION_DAYS, DEFAULT_IMPORT_RETENTION_DAYS);
		int batchSize = getPositiveIntGP(OpenConceptLabConstants.GP_IMPORT_CLEANUP_BATCH_SIZE, DEFAULT_IMPORT_CLEANUP_BATCH_SIZE);

		logger.info("Starting OCL import cleanup: retentionDays={}, batchSize={}", retentionDays, batchSize);
		int purged = Context.getService(ImportService.class).purgeOldImports(retentionDays, batchSize);
		logger.info("OCL import cleanup finished: purged {} import(s)", purged);
	}

	private int getPositiveIntGP(String globalProperty, int defaultValue) {
		String value = Context.getAdministrationService().getGlobalProperty(globalProperty);
		if (StringUtils.isBlank(value)) {
			return defaultValue;
		}

		try {
			int parsed = Integer.parseInt(value.trim());
			if (parsed > 0) {
				return parsed;
			}
		}
		catch (NumberFormatException nfe) {
			// fall through to warn + default
		}
		logger.warn("Global property {} must be a positive integer but was '{}'; using default {}",
		        globalProperty, value, defaultValue);
		return defaultValue;
	}
}