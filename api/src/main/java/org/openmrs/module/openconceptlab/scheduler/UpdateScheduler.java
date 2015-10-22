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

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.updater.Updater;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * It is used to run {@link Updater#run()} either as a scheduled task or on request.
 */
public class UpdateScheduler {
	
	public static final long DAY_PERIOD = 24 * 3600000;
	
	ThreadPoolTaskScheduler scheduler;
	
	ScheduledFuture<Updater> scheduledUpdate;
	
	Updater updater;
	
	UpdateService updateService;
	
    public void setScheduler(ThreadPoolTaskScheduler scheduler) {
	    this.scheduler = scheduler;
    }
    
    public void setUpdater(Updater updater) {
	    this.updater = updater;
    }
    
    public void setUpdateService(UpdateService updateService) {
	    this.updateService = updateService;
    }
	
	@SuppressWarnings("unchecked")
	public synchronized void schedule(Subscription subscription) {
		updateService.saveSubscription(subscription);
		
		if (scheduledUpdate != null) {
			scheduledUpdate.cancel(false);
		}
		
		if (subscription.getDays() != 0 || subscription.getHours() != 0 || subscription.getMinutes() != 0) {
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, subscription.getHours());
			calendar.set(Calendar.MINUTE, subscription.getMinutes());
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			
			scheduledUpdate = scheduler.scheduleAtFixedRate(updater, calendar.getTime(), subscription.getDays()
			        * DAY_PERIOD);
		}
	}
	
	public synchronized void unschedule() {
		updateService.unsubscribe();
		
		if (scheduledUpdate != null) {
			scheduledUpdate.cancel(false);
			scheduledUpdate = null;
		}
	}
	
	public void scheduleUpdate() {
		Subscription subscription = updateService.getSubscription();
		if (subscription != null) {
			schedule(subscription);
		}
	}
	
	public void scheduleNow() {
		if (updater.isRunning()) {
			throw new IllegalStateException("Cannot start the update, if there is another update in progress.");
		}
				
		scheduler.submit(updater);
		
		//delay at most 10 seconds until the update starts
		try {
			for (int i = 0; i < 100; i++) {
				Thread.sleep(100);
				if (updater.isRunning()) {
					break;
				}
			}
		} catch(InterruptedException ex) {
		    //ignore
		}
	}
}
