/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.page.controller;

import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Page controller for the details page
 */
public class DetailsPageController {
	
	public void get(PageModel model, @RequestParam(value = "updateId", required = false) Long updateId,
			@RequestParam(value = "debug", required = false) Boolean debug) {
		
		model.addAttribute("updateId", updateId);
		model.addAttribute("debug", debug);
	}
	
	public void post(PageModel model, @RequestParam(value = "updateId", required = false) Long updateId,
	        @RequestParam(value = "debug", required = false) Boolean debug,
	        @SpringBean("openconceptlab.updateService") ImportService updateService) {
		
				
		model.addAttribute("updateId", updateId);
		model.addAttribute("debug", debug);
	}
}
