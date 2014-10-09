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

		install(globalProperty(OpenConceptLabConstants.GP_SCHEDULE_DAYS, "Interval in days when the process is repeated ", null));

		install(globalProperty(OpenConceptLabConstants.GP_SCHEDULED_TIME, "The the time when the process should be carried on ", null));
	}
}
