package org.openmrs.module.openconceptlab.page.controller;

import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.module.openconceptlab.OpenConceptLabConstants;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Configuration page
 */
@AppPage(OpenConceptLabConstants.APP_OPEN_CONCEPT_LAB)
public class ConfigurePageController {
	public void controller(@RequestParam(required = false, value = "stateId") String stateId,
						   PageModel model) {
		model.addAttribute("state", stateId);
	}
}
