/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.fragment.controller;

import java.io.IOException;

import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateProgress;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;
import org.openmrs.module.openconceptlab.updater.Updater;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Fragment actions specifically for searching for OpenMRS objects
 */
public class StatusFragmentController {

	public UpdateProgress getUpdateProgress(@SpringBean("openconceptlab.updater") Updater updater, UiUtils ui) {
		
		return updater.getUpdateProgress();
	}

	public void runUpdates(@SpringBean("openconceptlab.updateScheduler") UpdateScheduler updateScheduler,
							@SpringBean("openconceptlab.updateService") UpdateService updateService,
							@RequestParam(required = false, value = "ignoreErrors") Boolean ignoreErrors) throws IOException {
		
		Update update = updateService.getLastUpdate();
		updateService.ignoreAllErrors(update);
		updateScheduler.scheduleNow();
	}
}
