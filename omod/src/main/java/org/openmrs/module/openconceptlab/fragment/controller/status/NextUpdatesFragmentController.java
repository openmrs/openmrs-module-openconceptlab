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
package org.openmrs.module.openconceptlab.fragment.controller.status;

import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.State;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateManager;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.Utils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Controller to schedule next updates
 */
public class NextUpdatesFragmentController {

	public void controller(FragmentModel model,
							@SpringBean UpdateService service,
							@SpringBean UpdateManager manager
						){
		Update update = new Update();
		Date lastUpdateLocalDateStopped;
		Integer days;
		Integer hours;
		Integer minutes;

		//fetch last update
		Update lastUpdate = service.getLastUpdate();
		Set<Item> updatedItems;
		List<Item> errorItems =  new ArrayList<Item>();
		//go through a set of items and find the number of items
		if(lastUpdate != null) {
			updatedItems = lastUpdate.getItems();
			errorItems = new ArrayList<Item>();
			//loop through and find those with errors
			for (Item item : updatedItems) {
				if (item.getState().equals(State.ERROR)) {
					errorItems.add(item);
				}
			}
		}


		if (service.getLastUpdate() == null) {
			lastUpdateLocalDateStopped = new Date();
		}
		else {
			lastUpdateLocalDateStopped = service.getLastUpdate().getLocalDateStopped();
		}

		if(service.getSubscription().getDays() == null) {
			days = 0;
		}
		else {
			days = service.getSubscription().getDays();
		}

		if ( service.getSubscription().getHours() == null) {
			hours = 0;
		}
		else {
			hours = service.getSubscription().getHours();
		}

		if (service.getSubscription().getMinutes() == null) {
			minutes = 0;
		}

		else {
			minutes = service.getSubscription().getMinutes();
		}
		model.addAttribute("nextUpdateDate", Utils.formatedDate(Utils.dateAddDays(lastUpdateLocalDateStopped, days)));
		model.addAttribute("nextUpdateTime", appendZeros(hours.toString()) +":"+appendZeros(minutes.toString()));
		model.addAttribute("errorItemSize", errorItems.size());

	}

	private String appendZeros(String k) {
		String results = k;
		if(k.length() < 2){
			results = "0"+k;
		}
		return results;
	}
}
