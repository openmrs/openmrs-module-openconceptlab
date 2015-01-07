/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.openconceptlab;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class UpdateServiceTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	UpdateService updateService;
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	/**
	 * @see UpdateService#getUpdate(Long)
	 * @verifies return update with id
	 */
	@Test
	public void getUpdate_shouldReturnUpdateWithId() throws Exception {
		Update newUpdate = new Update();
		updateService.startUpdate(newUpdate);
		
		Update update = updateService.getUpdate(newUpdate.getUpdateId());
		
		assertThat(update, is(newUpdate));
	}
	
	/**
	 * @see UpdateService#getUpdate(Long)
	 * @verifies throw IllegalArgumentException if update does not exist
	 */
	@Test
	public void getUpdate_shouldThrowIllegalArgumentExceptionIfUpdateDoesNotExist() throws Exception {
		exception.expect(IllegalArgumentException.class);
		updateService.getUpdate(0L);
		
	}
	
	/**
	 * @see UpdateService#getUpdatesInOrder()
	 * @verifies return all updates ordered descending by ids
	 */
	@Test
	public void getUpdatesInOrder_shouldReturnAllUpdatesOrderedDescendingByIds() throws Exception {
		Update firstUpdate = new Update();
		updateService.startUpdate(firstUpdate);
		updateService.stopUpdate(firstUpdate);
		
		Update secondUpdate = new Update();
		updateService.startUpdate(secondUpdate);
		
		List<Update> updatesInOrder = updateService.getUpdatesInOrder(0, 20);
		
		assertThat(updatesInOrder, contains(secondUpdate, firstUpdate));
	}

	/**
     * @see UpdateService#startUpdate(Update)
     * @verifies throw IllegalStateException if another update is in progress
     */
    @Test
    public void scheduleUpdate_shouldThrowIllegalStateExceptionIfAnotherUpdateIsInProgress() throws Exception {
    	Update firstUpdate = new Update();
    	updateService.startUpdate(firstUpdate);
    	
    	Update secondUpdate = new Update();
    	exception.expect(IllegalStateException.class);
    	updateService.startUpdate(secondUpdate);
    }

	/**
     * @see UpdateService#stopUpdate(Update)
     * @verifies throw IllegalArgumentException if not scheduled
     */
    @Test
    public void stopUpdate_shouldThrowIllegalArgumentExceptionIfNotScheduled() throws Exception {
    	Update update = new Update();
    	exception.expect(IllegalArgumentException.class);
    	updateService.stopUpdate(update);
    }

	/**
     * @see UpdateService#stopUpdate(Update)
     * @verifies throw IllegalStateException if trying to stop twice
     */
    @Test
    public void stopUpdate_shouldThrowIllegalStateExceptionIfTryingToStopTwice() throws Exception {
    	Update update = new Update();
    	updateService.startUpdate(update);
    	updateService.stopUpdate(update);
    	
    	exception.expect(IllegalStateException.class);
    	updateService.stopUpdate(update);
    }

	/**
	 * @see UpdateService#saveSubscription(Subscription)
	 * @Verifies saves the subscription
	 */
	@Test
	public void saveSubscription_shouldSaveSubscription() throws Exception {
		Subscription newSubscription = new Subscription();
		newSubscription.setUrl("http://openconceptlab.com/");
		newSubscription.setDays(5);
		newSubscription.setHours(3);
		newSubscription.setMinutes(30);
		newSubscription.setToken("c84e5a66d8b2e9a9bf1459cd81e6357f1c6a997e");

		updateService.saveSubscription(newSubscription);
		
		Subscription subscription = updateService.getSubscription();
		assertThat(subscription, is(newSubscription));
	}
}