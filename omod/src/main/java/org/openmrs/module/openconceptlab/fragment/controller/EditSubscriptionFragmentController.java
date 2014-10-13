package org.openmrs.module.openconceptlab.fragment.controller;

import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.OpenConceptLabConstants;
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
						   @RequestParam(value = "daysEdit", required = false) String daysEdit,
						   @RequestParam(value = "minutesEdit", required = false) String minutesEdit
	) {
		//populate the fields here
		populateFields(model);
		//check whether a subscription is made or NOT
		Boolean checkIfSubscribed;

		//save subscription to ocl- url, days and time
		editSubscription(urlEdit, daysEdit, hoursEdit, minutesEdit, optionsE);
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
	private void setGlobalPropertyUrl(String url) {
		if (url != null) {
			GlobalProperty gp_url = Context.getAdministrationService().getGlobalPropertyObject(OpenConceptLabConstants.GP_SUBSCRIPTION_URL);
			gp_url.setValue(url);
			Context.getAdministrationService().saveGlobalProperty(gp_url);
		}
	}

	private void setGlobalPropertyDays(String days) {
		if (days != null) {
			GlobalProperty gp_days = Context.getAdministrationService().getGlobalPropertyObject(OpenConceptLabConstants.GP_SCHEDULE_DAYS);
			gp_days.setValue(days);
			Context.getAdministrationService().saveGlobalProperty(gp_days);
		}
	}

	private void setGlobalPropertyTime(String h, String m) {
		if (h != null && m != null) {
			String time = h + ":" + m;
			GlobalProperty gp_time = Context.getAdministrationService().getGlobalPropertyObject(OpenConceptLabConstants.GP_SCHEDULED_TIME);
			gp_time.setValue(time);
			Context.getAdministrationService().saveGlobalProperty(gp_time);
		}
	}

	private void editSubscription(String url, String days, String hours, String minutes, String option) {
		//set the url
		setGlobalPropertyUrl(url);

		if( "ME".equals(option)) {
			//do what is required for manual processing

		}
		else if ("AE".equals(option)) {
			//set up an automatic subscription
			setGlobalPropertyDays(days);
			setGlobalPropertyTime(hours, minutes);
		}
	}
}
