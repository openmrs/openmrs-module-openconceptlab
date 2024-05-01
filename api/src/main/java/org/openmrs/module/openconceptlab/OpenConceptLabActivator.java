/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab;


import org.apache.commons.lang.StringUtils;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;
import org.openmrs.module.ModuleActivator;
import org.openmrs.module.openconceptlab.importer.Importer;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * This class contains the logic that is run every time this module is either started or stopped.
 */
public class OpenConceptLabActivator extends BaseModuleActivator implements DaemonTokenAware {

	private static final Logger log = LoggerFactory.getLogger(OpenConceptLabActivator.class);

	private static DaemonToken daemonToken;

	/**
	 * @see ModuleActivator#willRefreshContext()
	 */
	public void willRefreshContext() {
		log.info("Refreshing Open Concept Lab Module");
	}

	/**
	 * @see ModuleActivator#contextRefreshed()
	 */
	public void contextRefreshed() {
		if (!Context.isSessionOpen()) {
			Context.openSession();
		}

		markInProgressImportsAsFailed();

		String loadAtStartupPath = Context.getAdministrationService()
				.getGlobalProperty(OpenConceptLabConstants.GP_OCL_LOAD_AT_STARTUP_PATH);
		if (StringUtils.isBlank(loadAtStartupPath)) {
			loadAtStartupPath =
					new File(new File(new File(OpenmrsUtil.getApplicationDataDirectory(), "ocl"), "configuration"),
							"loadAtStartup").getAbsolutePath();
			Context.getAdministrationService()
					.saveGlobalProperty(
							new GlobalProperty(OpenConceptLabConstants.GP_OCL_LOAD_AT_STARTUP_PATH, loadAtStartupPath)
					);
		}

		File loadAtStartupDir = new File(loadAtStartupPath);
		if (!loadAtStartupDir.exists()) {
			loadAtStartupDir.mkdirs();
		}

		UpdateScheduler scheduler = Context.getRegisteredComponent("openconceptlab.updateScheduler", UpdateScheduler.class);

		File[] files = loadAtStartupDir.listFiles();
		if (files != null && files.length > 1) {
			throw new IllegalStateException(
					"There is more than one file in ocl/loadAtStartup directory\n" +
							"Ensure that there is only one file\n" +
							"Absolute directory path: " + loadAtStartupDir.getAbsolutePath());
		} else if (files != null && files.length != 0) {
			if (files[0].getName().endsWith(".zip")) {
				try {
					ZipFile zipFile = (new ZipFile(files[0]));
					Importer importer = Context.getRegisteredComponent("openconceptlab.importer", Importer.class);
					importer.run(zipFile);
				}
				catch (IOException e) {
					throw new IllegalStateException("Failed to open zip file", e);
				}
			} else {
				throw new IllegalStateException("File " + files[0].getName() + " must be in *.zip format");
			}
		}
		scheduler.scheduleUpdate();
		log.info("Open Concept Lab Module refreshed");
	}

	/**
	 * If OpenMRS is unexpectedly shutdown while an OCL Import is in progress, the import record in the database
	 * can be left in an "in progress" state, even though the import is no longer actually running.
	 * In this situation, we want to ensure these imports are marked as failed at the next startup, so that
	 * subsequent import attempts can succeed without errors that imports are already in progress
	 */
	private void markInProgressImportsAsFailed() {
		ImportService importService = Context.getService(ImportService.class);
		List<Import> inProgressImports = importService.getInProgressImports();
		if (!inProgressImports.isEmpty()) {
			log.warn("Found " + inProgressImports.size() + " in progress imports at startup");
			for (Import inProgressImport : inProgressImports) {
				log.warn("Updating Import#" + inProgressImport.getImportId() + " as failed");
				importService.failImport(inProgressImport, "System interruption during import");
				importService.stopImport(inProgressImport);
			}
		}
	}

	/**
	 * @see ModuleActivator#willStart()
	 */
	public void willStart() {
		log.info("Starting Open Concept Lab Module");
	}

	/**
	 * @see ModuleActivator#started()
	 */
	public void started() {
		log.info("Open Concept Lab Module started");
	}

	/**
	 * @see ModuleActivator#willStop()
	 */
	public void willStop() {
		log.info("Stopping Open Concept Lab Module");
	}

	/**
	 * @see ModuleActivator#stopped()
	 */
	public void stopped() {
		log.info("Open Concept Lab Module stopped");
	}

	@Override
	public void setDaemonToken(DaemonToken token) {
		daemonToken = token;
	}

	public static DaemonToken getDaemonToken() {
		return daemonToken;
	}

}
