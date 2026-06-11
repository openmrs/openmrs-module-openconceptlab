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

import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.OpenConceptLabConstants;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.util.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class ImportCleanupTaskSetup {

	private static final Logger log = LoggerFactory.getLogger(ImportCleanupTaskSetup.class);

	private ImportCleanupTaskSetup() {
	}

	public static void registerTask() {
		try {
			Context.addProxyPrivilege(PrivilegeConstants.MANAGE_SCHEDULER);
			SchedulerService schedulerService = Context.getSchedulerService();
			TaskDefinition task = schedulerService.getTaskByName(OpenConceptLabConstants.IMPORT_CLEANUP_TASK_NAME);
			if (task == null) {
				task = new TaskDefinition();
				task.setName(OpenConceptLabConstants.IMPORT_CLEANUP_TASK_NAME);
				task.setDescription("Purges old OCL import records and their items based on the "
				        + OpenConceptLabConstants.GP_IMPORT_RETENTION_DAYS + " global property. Enabled via "
				        + OpenConceptLabConstants.GP_IMPORT_CLEANUP_ENABLED + ".");
				task.setTaskClass(ImportCleanupTask.class.getName());
				task.setStartTime(new Date());
				task.setRepeatInterval(OpenConceptLabConstants.IMPORT_CLEANUP_TASK_INTERVAL);
				task.setStartOnStartup(true);
				schedulerService.saveTaskDefinition(task);
			}
			if (Boolean.TRUE.equals(task.getStartOnStartup())) {
				schedulerService.scheduleTask(task);
			}
			log.info("Registered scheduled task: " + OpenConceptLabConstants.IMPORT_CLEANUP_TASK_NAME);
		}
		catch (SchedulerException e) {
			log.error("Failed to schedule " + OpenConceptLabConstants.IMPORT_CLEANUP_TASK_NAME, e);
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_SCHEDULER);
		}
	}

	public static void shutdownTask() {
		try {
			Context.addProxyPrivilege(PrivilegeConstants.MANAGE_SCHEDULER);
			SchedulerService schedulerService = Context.getSchedulerService();
			TaskDefinition task = schedulerService.getTaskByName(OpenConceptLabConstants.IMPORT_CLEANUP_TASK_NAME);
			if (task != null) {
				schedulerService.shutdownTask(task);
			}
		}
		catch (Exception e) {
			log.warn("Failed to shut down " + OpenConceptLabConstants.IMPORT_CLEANUP_TASK_NAME + " on module stop", e);
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_SCHEDULER);
		}
	}
}