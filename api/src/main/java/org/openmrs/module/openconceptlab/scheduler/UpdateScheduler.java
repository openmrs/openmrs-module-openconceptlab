package org.openmrs.module.openconceptlab.scheduler;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.updater.Updater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

/**
 * It is used to run {@link Updater#run()} either as a scheduled task or on request.
 */
@Component("openconceptlab.updateScheduler")
public class UpdateScheduler {
	
	public static final long DAY_PERIOD = 24 * 3600000;
	
	@Autowired
	@Qualifier("openconceptlab.scheduler")
	ThreadPoolTaskScheduler scheduler;
	
	ScheduledFuture<UpdateDaemonRunner> scheduledUpdate;
	
	@Autowired
	UpdateDaemonRunner updaterDaemonRunner;
	
	@SuppressWarnings("unchecked")
	public synchronized void schedule(Subscription subscription) {
		if (scheduledUpdate != null) {
			scheduledUpdate.cancel(false);
		}
		
		if (subscription.getDays() != 0 || subscription.getHours() != 0 || subscription.getMinutes() != 0) {
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, subscription.getHours());
			calendar.set(Calendar.MINUTE, subscription.getMinutes());
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			
			scheduledUpdate = scheduler.scheduleAtFixedRate(updaterDaemonRunner, calendar.getTime(), subscription.getDays()
			        * DAY_PERIOD);
		}
	}
	
	public void scheduleNow() {
		scheduler.submit(updaterDaemonRunner);
		
		//delay at most 10 seconds until the update starts
		try {
			for (int i = 0; i < 100; i++) {
				Thread.sleep(100);
				if (updaterDaemonRunner.updater.isRunning()) {
					break;
				}
			}
		} catch(InterruptedException ex) {
		    //ignore
		}
	}
}
