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

import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.ui.framework.annotation.BindParams;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Configuration page
 */
public class ConfigurePageController {
	
	public void get(@SpringBean UpdateService updateService, PageModel model) {
		Subscription subscription = updateService.getSubscription();
		if (subscription == null) {
			subscription = new Subscription();
			subscription.setUrl("");
			subscription.setToken("");
			subscription.setDays(0);
			subscription.setHours(0);
			subscription.setMinutes(0);
		}
		model.put("subscription", subscription);
	}
	
	public void post(@SpringBean UpdateService updateService, @BindParams(value = "subscription") Subscription subscription,
	        @RequestParam(value = "unsubscribe", required = false) Boolean unsubscribe, PageModel model) {
		if (unsubscribe != null && unsubscribe) {
			subscription = new Subscription();
		}
		updateService.saveSubscription(subscription);
		model.put("subscription", subscription);
	}
}
