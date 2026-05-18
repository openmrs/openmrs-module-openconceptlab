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

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.openconceptlab.ItemCleanupService;
import org.openmrs.module.openconceptlab.OpenConceptLabActivator;
import org.openmrs.module.openconceptlab.OpenConceptLabConstants;
import org.openmrs.api.AdministrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages debounced scheduling of item cleanup after imports complete.
 * Each call to {@link #scheduleCleanup()} cancels any previously scheduled cleanup
 * and reschedules it after the configured delay, ensuring that rapid successive
 * imports (e.g., during startup) only trigger a single cleanup run.
 */
public class CleanupScheduler {

	private static final Logger log = LoggerFactory.getLogger(CleanupScheduler.class);

	private static final int DEFAULT_DELAY_MINUTES = 5;

	private static final int MAX_RETRIES = 1;

	ThreadPoolTaskScheduler scheduler;

	ItemCleanupService cleanupService;

	AdministrationService adminService;

	private volatile ScheduledFuture<?> scheduledCleanup;

	private final AtomicInteger retryCount = new AtomicInteger(0);

	public void setScheduler(ThreadPoolTaskScheduler scheduler) {
		this.scheduler = scheduler;
	}

	public void setCleanupService(ItemCleanupService cleanupService) {
		this.cleanupService = cleanupService;
	}

	public void setAdminService(AdministrationService adminService) {
		this.adminService = adminService;
	}

	/**
	 * Schedules cleanup to run after the configured delay.
	 * If a cleanup is already pending, it is cancelled and rescheduled (debounce).
	 */
	public synchronized void scheduleCleanup() {
		if (scheduledCleanup != null && !scheduledCleanup.isDone()) {
			log.debug("Cancelling previously scheduled cleanup (debounce)");
			scheduledCleanup.cancel(false);
		}

		int delayMinutes = getCleanupDelayMinutes();
		Date runAt = new Date(System.currentTimeMillis() + (delayMinutes * 60 * 1000L));
		log.info("Scheduling item cleanup to run at {}", runAt);

		scheduledCleanup = scheduler.schedule(this::runCleanup, runAt);
	}

	private void runCleanup() {
		DaemonToken token = OpenConceptLabActivator.getDaemonToken();
		if (token == null) {
			log.warn("Daemon token not available, skipping item cleanup. Module may have been stopped.");
			return;
		}

		Daemon.runInDaemonThreadAndWait(() -> {
			try {
				cleanupService.runCleanup();
				retryCount.set(0);
			}
			catch (Exception e) {
				log.error("Error during item cleanup", e);
				if (retryCount.incrementAndGet() <= MAX_RETRIES) {
					log.info("Scheduling cleanup retry (attempt {})", retryCount.get());
					scheduleCleanup();
				} else {
					log.error("Item cleanup failed after {} retries. Will retry after the next import.", MAX_RETRIES);
					retryCount.set(0);
				}
			}
		}, token);
	}

	private int getCleanupDelayMinutes() {
		String value = adminService.getGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_DELAY_MINUTES);
		if (StringUtils.isNotBlank(value)) {
			try {
				int minutes = Integer.parseInt(value.trim());
				if (minutes > 0) {
					return minutes;
				}
				log.warn("Cleanup delay minutes must be positive, got: '{}'. Using default: {}", value, DEFAULT_DELAY_MINUTES);
			}
			catch (NumberFormatException e) {
				log.warn("Invalid cleanup delay minutes: '{}'. Using default: {}", value, DEFAULT_DELAY_MINUTES);
			}
		}
		return DEFAULT_DELAY_MINUTES;
	}
}
