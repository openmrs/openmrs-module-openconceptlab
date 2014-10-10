package org.openmrs.module.openconceptlab.page.controller;

import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
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
						   @RequestParam(value = "url-sub", required = false) String urlSub,
						   @RequestParam(value = "manualS", required = false) String manualS,
						   @RequestParam(value = "autoS", required = false) String autoS,
						   @RequestParam(value = "hoursSub", required = false) String hoursSub,
						   @RequestParam(value = "daysSub", required = false) String daysSub,
						   @RequestParam(value = "minutesSub", required = false) String minutesSub
						   ) {
		//populate the fields here
		populateFields(model);
		//check whether a subscription is made or NOT
		Boolean checkIfSubscribed;

		Update update = updateService.getLastUpdate();
		if( update != null){
			checkIfSubscribed = true;
		}
		else {
			checkIfSubscribed = false;
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
