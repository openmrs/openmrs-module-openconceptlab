/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.scheduler;

import org.openmrs.api.context.Daemon;
import org.openmrs.module.openconceptlab.OpenConceptLabActivator;
import org.openmrs.module.openconceptlab.updater.Updater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The sole purpose of this class is to run the update in a thread with elevated privileges.
 */
@Component("openconceptlab.updateDaemonRunner")
public class UpdateDaemonRunner implements Runnable {
	
	@Autowired
	Updater updater;
	
	@Override
	public void run() {
		Daemon.runInDaemonThreadAndWait(updater, OpenConceptLabActivator.getDaemonToken());
	}
}
