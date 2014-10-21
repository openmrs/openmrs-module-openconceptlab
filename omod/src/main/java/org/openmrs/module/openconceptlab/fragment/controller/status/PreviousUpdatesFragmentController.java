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
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.Utils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Controller to run previous updates
 */
public class PreviousUpdatesFragmentController {
			public void controller(FragmentModel model,
								   @SpringBean UpdateService service
			){
				List<Update> allUpdates = service.getUpdatesInOrder();
				SortedSet<Item> items = new TreeSet<Item>();
				Integer errors = 0;
				Long duration = null;

				for(Update update: allUpdates) {
					items.addAll(update.getItems());
				}
				//loop through each item object to count errors
				for( Item item : items) {
					if(item != null) {
						if (item.getState().equals(State.ERROR)) {
							errors++;
						}
						duration = Utils.dateDifference(item.getUpdate().getLocalDateStopped(), item.getUpdate().getLocalDateStarted(), TimeUnit.MINUTES);
					}
				}
				model.addAttribute("items", items);
				if( errors > 0) {
					model.addAttribute("errors", errors+" errors");
				}
				else {
					model.addAttribute("errors", "OK");
				}

				model.addAttribute("duration", duration);
			}
}
