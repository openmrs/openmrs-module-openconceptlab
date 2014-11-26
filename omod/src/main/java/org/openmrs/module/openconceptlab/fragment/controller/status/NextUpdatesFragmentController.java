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
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.Update;
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
	
	public void controller(FragmentModel model, @SpringBean UpdateService service) {
		Update lastUpdate = service.getLastUpdate();
		Set<Item> updatedItems;
		List<Item> errorItems = new ArrayList<Item>();
		if (lastUpdate != null) {
			updatedItems = lastUpdate.getItems();
			errorItems = new ArrayList<Item>();
			for (Item item : updatedItems) {
				if (item.getState().equals(State.ERROR)) {
					errorItems.add(item);
				}
			}
		}
		
		Date lastUpdateDate;
		Subscription subscription = service.getSubscription();
		boolean manual = false;
		if (lastUpdate == null) {
			lastUpdateDate = new Date();
		} else {
			lastUpdateDate = service.getLastUpdate().getLocalDateStopped();
		}
		
		Integer days = 0;
		Integer hours = 0;
		Integer minutes = 0;
		
		if (service.getSubscription() != null) {
			days = service.getSubscription().getDays();
			hours = service.getSubscription().getHours();
			minutes = service.getSubscription().getMinutes();
		}

		if (subscription.isSubscribed() && (subscription.getDays() == null || subscription.getDays() == 0)) {
			manual = true;
		}
		
		model.addAttribute("nextUpdateDate", Utils.formatedDate(Utils.dateAddDays(lastUpdateDate, days)));
		model.addAttribute("nextUpdateTime", appendZeros(hours.toString()) + ":" + appendZeros(minutes.toString()));
		model.addAttribute("errorItemSize", errorItems.size());
		model.addAttribute("manual", manual);
		
	}
	
	private String appendZeros(String k) {
		String results = k;
		if (k.length() < 2) {
			results = "0" + k;
		}
		return results;
	}
}
