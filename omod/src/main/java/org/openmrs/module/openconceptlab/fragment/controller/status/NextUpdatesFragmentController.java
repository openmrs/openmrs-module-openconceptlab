/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.fragment.controller.status;

import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.Utils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Controller to schedule next updates
 */
public class NextUpdatesFragmentController {
	
	public void controller(FragmentModel model, @SpringBean UpdateService service) {

		Update lastUpdate = service.getLastUpdate();

		Set<ItemState> states = new HashSet<ItemState>();
		states.add(ItemState.ERROR);
		int errorsItems = 0;
		if (lastUpdate != null) {
			model.addAttribute("lastUpdateId", lastUpdate.getUpdateId());
			errorsItems = service.getUpdateItemsCount(lastUpdate, states);
		}
		Boolean isLastUpdateSuccessful = service.isLastUpdateSuccessful();
		
		Date lastUpdateDate;
		Subscription subscription = service.getSubscription();
		boolean manual = false;
		if (lastUpdate == null) {
			lastUpdateDate = new Date();
		} else {
			lastUpdateDate = service.getLastUpdate().getLocalDateStarted();
		}
		
		Integer days = 0;
		Integer hours = 0;
		Integer minutes = 0;
		
		if (service.getSubscription() != null) {
			days = service.getSubscription().getDays();
			hours = service.getSubscription().getHours();
			minutes = service.getSubscription().getMinutes();
		}

		if (subscription != null && subscription.isManual()) {
			manual = true;
		}
		
		
		model.addAttribute("nextUpdateDate", Utils.formatedDate(Utils.dateAddDays(lastUpdateDate, days)));
		model.addAttribute("nextUpdateTime", appendZeros(hours.toString()) + ":" + appendZeros(minutes.toString()));
		model.addAttribute("errorItemSize", errorsItems);
		model.addAttribute("manual", manual);
		model.addAttribute("isLastUpdateSuccessful", isLastUpdateSuccessful);
		
	}
	
	private String appendZeros(String k) {
		String results = k;
		if (k.length() < 2) {
			results = "0" + k;
		}
		return results;
	}
}
