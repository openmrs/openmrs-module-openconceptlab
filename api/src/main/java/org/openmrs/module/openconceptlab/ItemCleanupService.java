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

import org.openmrs.annotation.Authorized;
import org.openmrs.util.PrivilegeConstants;

public interface ItemCleanupService {

	/**
	 * Executes the cleanup process:
	 * <ol>
	 *   <li>Identifies imports eligible for cleanup based on the configured retention policy</li>
	 *   <li>Preserves at least one non-error Item per unique URL (concept/mapping), if one exists</li>
	 *   <li>Deletes expired items in batches</li>
	 *   <li>Deletes orphaned Import records that have no remaining Items</li>
	 * </ol>
	 *
	 * Does nothing if no retention policy is configured.
	 *
	 * @return the number of items deleted, or 0 if cleanup is disabled or nothing to clean
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	int runCleanup();
}
