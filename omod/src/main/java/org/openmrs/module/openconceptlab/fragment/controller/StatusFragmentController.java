/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.openconceptlab.fragment.controller;

import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.UpdateManager;
import org.openmrs.module.openconceptlab.UpdateProgress;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;

import java.io.IOException;

/**
 * Fragment actions specifically for searching for OpenMRS objects
 */
public class StatusFragmentController {

	public UpdateProgress getUpdateProgress(@SpringBean("updateService") UpdateService updateService, UiUtils ui) {

		return updateService.getUpdateProgress();
	}

	public void unsubscribe(@SpringBean("updateService") UpdateService service ) {
		Subscription subscription = service.getSubscription();
		subscription.setUrl("");
		subscription.setDays(null);
		subscription.setHours(null);
		subscription.setMinutes(null);

		//save the subscription
		service.saveSubscription(subscription);
	}

	public void runUpdates(@SpringBean("updateService") UpdateService updateService,
							 @SpringBean("updateManager") UpdateManager updateManager,
							 UiUtils ui) throws IOException {
		if (updateService.getLastUpdate() != null && updateService.getLastUpdate().isStopped()) {
			updateManager.runUpdate();
		}
	}
}
