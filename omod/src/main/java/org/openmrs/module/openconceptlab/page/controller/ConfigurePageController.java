package org.openmrs.module.openconceptlab.page.controller;

import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.OpenConceptLabConstants;
import org.openmrs.ui.framework.page.PageModel;

/**
 * Configuration page
 */
public class ConfigurePageController {
	public void controller(PageModel model){

		Boolean checkIfSubscribed = true;
		String gp_url = getGlobalPropertyUrl();

		if( gp_url == null || gp_url.isEmpty()){
			checkIfSubscribed = false;
		}
		model.addAttribute("checkIfSubscribed", checkIfSubscribed);
	}
	private
	String getGlobalPropertyUrl() {
		GlobalProperty gp_url = Context.getAdministrationService().getGlobalPropertyObject(OpenConceptLabConstants.GP_SUBSCRIPTION_URL);
		return gp_url.getPropertyValue();
	}
}
