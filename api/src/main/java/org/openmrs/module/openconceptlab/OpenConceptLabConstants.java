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
	public static final String GP_SUBSCRIPTION_URL = MODULE_ID + ".subscriptionUrl";
	public static final String GP_SCHEDULED_DAYS = MODULE_ID + ".scheduledDays";
	public static final String GP_SCHEDULED_TIME = MODULE_ID + ".scheduledTime";
	public static final String GP_TOKEN = MODULE_ID + ".token";
}
