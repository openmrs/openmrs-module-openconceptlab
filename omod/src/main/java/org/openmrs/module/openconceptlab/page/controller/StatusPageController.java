/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.page.controller;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.openconceptlab.Subscription;
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
		
		if (service.isUpdateRunning()) {
			checkIfUpdatesIsRunning = true;
		}

		model.addAttribute("checkIfUpdatesIsRunning", checkIfUpdatesIsRunning);
		model.addAttribute("checkIfSubscribed", checkIfSubscribed);
	}
}
