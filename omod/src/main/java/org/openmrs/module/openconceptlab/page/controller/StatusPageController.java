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
package org.openmrs.module.openconceptlab.page.controller;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

/**
 * Created by codehub on 15/10/14.
 */
public class StatusPageController {
	public void controller(PageModel model,
						   @SpringBean UpdateService service){

		boolean checkIfUpdatesIsRunning = false;
		boolean checkIfSubscribed = true;

		Subscription subscription = service.getSubscription();
		String subscription_url = "";
		if(subscription != null) {
			subscription_url = subscription.getUrl();
		}
		if(StringUtils.isEmpty(subscription_url)) {
			checkIfSubscribed = false;
		}
		Update lastUpdate = service.getLastUpdate();
		if (lastUpdate != null && !lastUpdate.isStopped()) {
			checkIfUpdatesIsRunning = true;
		}

		model.addAttribute("checkIfUpdatesIsRunning", checkIfUpdatesIsRunning);
		model.addAttribute("checkIfSubscribed", checkIfSubscribed);
	}
}
