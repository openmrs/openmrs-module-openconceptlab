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

import java.util.UUID;

/**
 * Contains OpenConceptLab Constants
 */
public class OpenConceptLabConstants {
	/**
	 * Module ID
	 */
	public static final String MODULE_ID = "openconceptlab";

	/**
	 * App IDs
	 */
	public static final String APP_OPEN_CONCEPT_LAB = MODULE_ID + ".configure";

	/**
	 * Global property names
	 */
	public static final String GP_SUBSCRIPTION_UUID = MODULE_ID + ".subscriptionUuid";
	public static final String GP_SUBSCRIPTION_URL = MODULE_ID + ".subscriptionUrl";
	public static final String GP_SCHEDULED_DAYS = MODULE_ID + ".scheduledDays";
	public static final String GP_SCHEDULED_TIME = MODULE_ID + ".scheduledTime";
	public static final String GP_TOKEN = MODULE_ID + ".token";
	public static final String GP_SUBSCRIBED_TO_SNAPSHOT = MODULE_ID + ".subscribedToSnapshot";
	public static final String GP_VALIDATION_TYPE = MODULE_ID + ".validationType";
	public static final String GP_OCL_LOAD_AT_STARTUP_PATH = MODULE_ID + ".oclLoadAtStartupPath";
	public static final String GP_IMPORT_CLEANUP_ENABLED = MODULE_ID + ".importCleanupEnabled";
	public static final String GP_IMPORT_RETENTION_DAYS = MODULE_ID + ".importRetentionDays";
	public static final String GP_IMPORT_CLEANUP_BATCH_SIZE = MODULE_ID + ".importCleanupBatchSize";

	/**
	 * Import cleanup scheduled task
	 */
	public static final String IMPORT_CLEANUP_TASK_NAME = "Open Concept Lab Import Cleanup";
	public static final long IMPORT_CLEANUP_TASK_INTERVAL = 86400L;

	public static final UUID OPEN_CONCEPT_LAB_NAMESPACE_UUID = UUID.fromString("672c6cb2-4f47-4cb0-9fca-b5f6116cd33a");

	/**
	 * Default retire reason
	 */
	public static final String DEFAULT_RETIRE_REASON = "Retired in OCL";
}
