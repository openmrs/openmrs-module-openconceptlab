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
package org.openmrs.module.openconceptlab.fragment.controller;

import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.State;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateManager;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Fragment controller to list all the errors in the last update.
 */
public class LastUpdateErrorsFragmentController {
			public void controller(FragmentModel model,
								   @SpringBean UpdateService service,
								   @SpringBean UpdateManager manager
			){
				//fetch last update
				Update lastUpdate = service.getLastUpdate();
				Set<Item> updatedItems;
				List<Item> errorItems = new ArrayList<Item>();
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
				model.addAttribute("allErrorItems", errorItems);
			}
}
