package org.openmrs.module.openconceptlab;

import org.openmrs.api.context.Daemon;
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
