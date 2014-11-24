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

import org.apache.commons.lang.StringUtils;
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration page
 */
public class ConfigurePageController {
	public void controller(PageModel model,
						   @SpringBean UpdateService updateService,
						   @RequestParam(value = "urlSub", required = false) String urlSub,
						   @RequestParam(value = "token", required = false) String token,
						   @RequestParam(value = "option", required = false) String option,
						   @RequestParam(value = "hoursSub", required = false) String hoursSub,
						   @RequestParam(value = "daysSub", required = false) Integer daysSub,
						   @RequestParam(value = "minutesSub", required = false) String minutesSub,
						   UiUtils ui
							){
		//populate the fields here
		populateFields(model);

		boolean checkIfSubscribed;
		String gp_url = null;
		if(updateService.getSubscription() != null){
			gp_url = updateService.getSubscription().getUrl();
		}

		if(StringUtils.isEmpty(gp_url)){
			checkIfSubscribed = false;
		}
		else {
			checkIfSubscribed = true;
		}
		//check if there are any subscription
		Subscription subscriptionToEdit = updateService.getSubscription();
		if(subscriptionToEdit != null) {
			model.addAttribute("subscriptionToEdit", subscriptionToEdit);
		}

		//save subscription to ocl- url, days and time
		//create subscription object
		if (StringUtils.isNotEmpty(urlSub)) {
			Subscription subscription = new Subscription();
			subscription.setUrl(urlSub);
			subscription.setToken(token);
			if ("A".equals(option)) {
				subscription.setDays(daysSub);
				subscription.setHours(Integer.parseInt(hoursSub));
				subscription.setMinutes(Integer.parseInt(minutesSub));
			}
			updateService.saveSubscription(subscription);
		}

		model.addAttribute("checkIfSubscribed", checkIfSubscribed);
}

	//method to populate the days, hours and minutes
	private void populateFields(PageModel model) {
		List<Integer> populateDays = new ArrayList<Integer>();
		for(int i=1;i<21;i++) {
			populateDays.add(i);
		}

		List<String> populateHrs = new ArrayList<String>();
		for(Integer k=1; k<24; k++) {
			if(k.toString().length() < 2) {
				populateHrs.add("0" +k);
			}
			else {
				populateHrs.add(k.toString());
			}
		}

		List<String> populateMinutes = new ArrayList<String>();
		for(Integer m=1; m<60; m++) {
			if(m.toString().length() < 2) {
				populateMinutes.add("0" +m);
			}
			else {
				populateMinutes.add(m.toString());
			}
		}

		model.addAttribute("populateDays", populateDays);
		model.addAttribute("populateHrs", populateHrs);
		model.addAttribute("populateMinutes", populateMinutes);
	}
}
