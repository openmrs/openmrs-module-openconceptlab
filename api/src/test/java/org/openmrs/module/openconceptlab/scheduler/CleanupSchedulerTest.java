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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.api.AdministrationService;
import org.openmrs.module.openconceptlab.ItemCleanupService;
import org.openmrs.module.openconceptlab.OpenConceptLabConstants;
import org.openmrs.test.BaseContextMockTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CleanupSchedulerTest extends BaseContextMockTest {

	@Mock
	ThreadPoolTaskScheduler scheduler;

	@Mock
	ItemCleanupService cleanupService;

	@Mock
	AdministrationService adminService;

	// Not @Mock — created manually to prevent @InjectMocks from injecting it
	// into CleanupScheduler's scheduledCleanup field by type
	ScheduledFuture<?> scheduledFuture;

	@InjectMocks
	CleanupScheduler cleanupScheduler;

	@Before
	public void before() {
		scheduledFuture = Mockito.mock(ScheduledFuture.class);
		doReturn(scheduledFuture).when(scheduler).schedule(any(Runnable.class), any(Date.class));
	}

	@Test
	public void scheduleCleanup_shouldScheduleWithDefaultDelay() {
		when(adminService.getGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_DELAY_MINUTES)).thenReturn(null);

		long before = System.currentTimeMillis() + (5 * 60 * 1000L);
		cleanupScheduler.scheduleCleanup();
		long after = System.currentTimeMillis() + (5 * 60 * 1000L);

		ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
		verify(scheduler).schedule(any(Runnable.class), dateCaptor.capture());

		long scheduledTime = dateCaptor.getValue().getTime();
		assertThat(scheduledTime, greaterThanOrEqualTo(before));
		assertThat(scheduledTime, lessThanOrEqualTo(after));
	}

	@Test
	public void scheduleCleanup_shouldScheduleWithConfiguredDelay() {
		when(adminService.getGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_DELAY_MINUTES)).thenReturn("10");

		long before = System.currentTimeMillis() + (10 * 60 * 1000L);
		cleanupScheduler.scheduleCleanup();
		long after = System.currentTimeMillis() + (10 * 60 * 1000L);

		ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
		verify(scheduler).schedule(any(Runnable.class), dateCaptor.capture());

		long scheduledTime = dateCaptor.getValue().getTime();
		assertThat(scheduledTime, greaterThanOrEqualTo(before));
		assertThat(scheduledTime, lessThanOrEqualTo(after));
	}

	@Test
	public void scheduleCleanup_shouldUseDefaultDelayForInvalidProperty() {
		when(adminService.getGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_DELAY_MINUTES)).thenReturn("not-a-number");

		long before = System.currentTimeMillis() + (5 * 60 * 1000L);
		cleanupScheduler.scheduleCleanup();
		long after = System.currentTimeMillis() + (5 * 60 * 1000L);

		ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
		verify(scheduler).schedule(any(Runnable.class), dateCaptor.capture());

		long scheduledTime = dateCaptor.getValue().getTime();
		assertThat(scheduledTime, greaterThanOrEqualTo(before));
		assertThat(scheduledTime, lessThanOrEqualTo(after));
	}

	@Test
	public void scheduleCleanup_shouldCancelPreviouslyScheduledCleanup() {
		when(adminService.getGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_DELAY_MINUTES)).thenReturn("5");
		when(scheduledFuture.isDone()).thenReturn(false);

		cleanupScheduler.scheduleCleanup();
		cleanupScheduler.scheduleCleanup();

		// First call schedules, second call cancels + reschedules
		verify(scheduledFuture).cancel(false);
		verify(scheduler, times(2)).schedule(any(Runnable.class), any(Date.class));
	}

	@Test
	public void scheduleCleanup_shouldNotCancelAlreadyCompletedCleanup() {
		when(adminService.getGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_DELAY_MINUTES)).thenReturn("5");
		when(scheduledFuture.isDone()).thenReturn(true);

		cleanupScheduler.scheduleCleanup();
		cleanupScheduler.scheduleCleanup();

		// First completed, so second should not cancel it
		verify(scheduledFuture, never()).cancel(false);
		verify(scheduler, times(2)).schedule(any(Runnable.class), any(Date.class));
	}
}
