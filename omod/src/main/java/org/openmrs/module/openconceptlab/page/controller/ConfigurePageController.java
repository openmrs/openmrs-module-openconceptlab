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

import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;
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
			subscription = newBlankSubscription();
		}
		model.put("subscription", subscription);
	}

	private Subscription newBlankSubscription() {
	    Subscription subscription;
	    subscription = new Subscription();
	    subscription.setUrl("");
	    subscription.setToken("");
	    subscription.setDays(0);
	    subscription.setHours(0);
	    subscription.setMinutes(0);
	    return subscription;
    }
	
	public String post(@SpringBean UpdateScheduler updateScheduler, @BindParams(value = "subscription") Subscription subscription,
	        @RequestParam(value = "unsubscribe", required = false) Boolean unsubscribe, PageModel model) {
		if (unsubscribe != null && unsubscribe) {
			updateScheduler.unschedule();
			subscription = newBlankSubscription();
			
		} else {
			updateScheduler.schedule(subscription);
		}
		model.put("subscription", subscription);
		
		return "redirect:openconceptlab/status.page";
	}
}
