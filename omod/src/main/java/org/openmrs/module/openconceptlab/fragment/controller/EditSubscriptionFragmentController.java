package org.openmrs.module.openconceptlab.fragment.controller;

import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by codehub on 13/10/14.
 */
public class EditSubscriptionFragmentController {
	public void controller(FragmentModel model,
						   @SpringBean UpdateService updateService,
						   @RequestParam(value = "urlEdit", required = false) String urlEdit,
						   @RequestParam(value = "optionsE", required = false) String optionsE,
						   @RequestParam(value = "hoursEdit", required = false) String hoursEdit,
						   @RequestParam(value = "daysEdit", required = false) Integer daysEdit,
						   @RequestParam(value = "minutesEdit", required = false) String minutesEdit
	) {
		//populate the fields here
		populateFields(model);
		//save edited subscription to ocl- url, days and time
		Subscription subscriptionE = new Subscription();
		subscriptionE.setUrl(urlEdit);
		if ("AE".equals(optionsE)) {
			subscriptionE.setDays(daysEdit);
			subscriptionE.setHours(Integer.parseInt(hoursEdit));
			subscriptionE.setMinutes(Integer.parseInt(minutesEdit));
		}
		updateService.saveSubscription(subscriptionE);
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
