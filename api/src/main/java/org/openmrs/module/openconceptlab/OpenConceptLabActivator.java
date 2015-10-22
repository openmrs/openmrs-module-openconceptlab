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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;
import org.openmrs.module.ModuleActivator;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;

/**
 * This class contains the logic that is run every time this module is either started or stopped.
 */
public class OpenConceptLabActivator implements ModuleActivator, DaemonTokenAware {
	
	protected Log log = LogFactory.getLog(getClass());
	
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
			
		UpdateScheduler scheduler = Context.getRegisteredComponent("openconceptlab.updateScheduler", UpdateScheduler.class);
		scheduler .scheduleUpdate();
		
		log.info("Open Concept Lab Module refreshed");
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
