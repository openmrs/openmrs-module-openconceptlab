package org.openmrs.module.openconceptlab.fragment.controller;

import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

public class SubscribeToOclFragmentController {
	public void controller(FragmentModel model,
						   @SpringBean UpdateService updateService,
						   @RequestParam(value = "urlSub", required = false) String urlSub,
						   @RequestParam(value = "option", required = false) String option,
						   @RequestParam(value = "hoursSub", required = false) String hoursSub,
						   @RequestParam(value = "daysSub", required = false) Integer daysSub,
						   @RequestParam(value = "minutesSub", required = false) String minutesSub
						) {
		//populate the fields here
			populateFields(model);
		//save subscription to ocl- url, days and time
		//create subscription object
		Subscription subscription = new Subscription();
		subscription.setUrl(urlSub);
		if ("A".equals(option)) {
			subscription.setDays(daysSub);
			subscription.setHours(Integer.parseInt(hoursSub));
			subscription.setMinutes(Integer.parseInt(minutesSub));
		}
		updateService.saveSubscription(subscription);
	}

	//method to populate the days, hours and minutes
	private void populateFields(FragmentModel model) {
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
