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

import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;
import org.springframework.stereotype.Component;

import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.globalProperty;

/**
 * Common metadata bundle
 */
@Component
public class OpenConceptLabMetaData extends AbstractMetadataBundle {

	/**
	 * @see org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle#install()
	 */
	@Override
	public void install() {
		install(globalProperty(OpenConceptLabConstants.GP_SUBSCRIPTION_URL, "The subscription URL to be persisted is configured ", null));

		install(globalProperty(OpenConceptLabConstants.GP_SCHEDULED_DAYS, "Interval in days when the process is repeated ", null));

		install(globalProperty(OpenConceptLabConstants.GP_SCHEDULED_TIME, "The the time when the process should be carried on ", null));

		install(globalProperty(OpenConceptLabConstants.GP_TOKEN, "The API Token ", null));
	}
}
