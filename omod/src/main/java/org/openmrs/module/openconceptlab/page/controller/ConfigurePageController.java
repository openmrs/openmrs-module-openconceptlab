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

import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.ui.framework.annotation.BindParams;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

/**
 * Configuration page
 */
public class ConfigurePageController {
	
	public void get(@SpringBean UpdateService updateService, PageModel model) {
		model.put("subscription", updateService.getSubscription());
	}
	
	public void post(@SpringBean UpdateService updateService, @BindParams(value = "subscription") Subscription subscription,
	        PageModel model) {
		updateService.saveSubscription(subscription);
		model.put("subscription", subscription);
	}
}
