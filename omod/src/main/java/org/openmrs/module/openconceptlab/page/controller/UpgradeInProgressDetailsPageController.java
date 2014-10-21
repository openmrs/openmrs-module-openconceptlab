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
package org.openmrs.module.openconceptlab.page.controller;

import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Page Controller to hold upgrade informatio
 */
public class UpgradeInProgressDetailsPageController {

	public void controller(PageModel model,
						   @SpringBean UpdateService service){
		Update updatedItems = service.getLastUpdate();
		SortedSet<Item> items = new TreeSet<Item>();
		if(updatedItems != null){
			items.addAll(updatedItems.getItems());
		}
		model.addAttribute("updatedItems", items);
	}
}
