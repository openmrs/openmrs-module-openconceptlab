package org.openmrs.module.openconceptlab.page.controller;

import org.openmrs.api.context.Context;
import org.openmrs.module.appframework.domain.AppDescriptor;
import org.openmrs.module.appframework.service.AppFrameworkService;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.util.OpenmrsUtil;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Configuration home page
 */
public class HomePageController {

	public String controller(PageModel model, UiUtils ui, HttpSession session, @SpringBean KenyaUiUtils kenyaUi) {

		// Get apps for the current user
		List<AppDescriptor> apps = Context.getService(AppFrameworkService.class).getAppsForCurrentUser();

		// Sort by order property
		Collections.sort(apps, new Comparator<AppDescriptor>() {
			@Override
			public int compare(AppDescriptor left, AppDescriptor right) {
				return OpenmrsUtil.compareWithNullAsGreatest(left.getOrder(), right.getOrder());
			}
		});

		model.addAttribute("apps", apps);

		return null;
	}
}
