package org.openmrs.module.openconceptlab.fragment.controller;

import org.openmrs.module.kenyaui.form.AbstractWebForm;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by codehub on 08/10/14.
 */
public class ConfigureFragmentController {
	/**
	 *
	 */
	public void controller(FragmentModel model,
						   @FragmentParam(value = "stateId", required = false) Boolean stateId) {
		Boolean state = stateId != null ? stateId : false;

		model.addAttribute("command", newEditSubscriptionForm(state));

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

	/**
	 * Subscribe and update the subscription to ocl
	 * @param state the state
	 * @return the form
	 */
	public EditSubscriptionForm newEditSubscriptionForm(@RequestParam(value = "stateId", required = false) Boolean state) {
		if (state) {
			return new EditSubscriptionForm(state); // For editing existing subscription url
		} else {
			return new EditSubscriptionForm(state); // For creating new subscription to ocl
		}
	}

	/**
	 * The form command object for editing patients
	 */
	public class EditSubscriptionForm extends AbstractWebForm {

		private String url;
		private String install;
		private String days;
		private String hours;
		private String minutes;
		/**
		 * Creates an edit form for a new subscription
		 */
		public EditSubscriptionForm() {

		}

		/**
		 * Creates an edit form for an existing subscription url
		 */
		public EditSubscriptionForm(Boolean state) {
			this();
		}

		/**
		 * Saves the form
		 */
		@Override
		public Object save() {
			return null;
		}

		@Override
		public void validate(Object o, Errors errors) {

		}

		/**
		 *
		 * @return the subscription url
		 */
		public String getUrl() {
			return url;
		}

		/**
		 *
		 * @param url the url set
		 */
		public void setUrl(String url) {
			this.url = url;
		}

		public String getInstall() {
			return install;
		}

		public void setInstall(String install) {
			this.install = install;
		}
		public String getHours() {
			return hours;
		}

		public void setHours(String hours) {
			this.hours = hours;
		}

		public String getMinutes() {
			return minutes;
		}

		public void setMinutes(String minutes) {
			this.minutes = minutes;
		}

		public String getDays() {
			return days;
		}

		public void setDays(String days) {
			this.days = days;
		}
	}
}
