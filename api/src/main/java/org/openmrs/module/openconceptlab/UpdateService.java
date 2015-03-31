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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;
import org.openmrs.module.openconceptlab.updater.Updater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("openconceptlab.updateService")
public class UpdateService {
	
	@Autowired
	SessionFactory sessionFactory;
	
	@Autowired
	@Qualifier("adminService")
	AdministrationService adminService;
	
	@Autowired
	UpdateScheduler scheduler;
	
	@Autowired
	@Qualifier("openconceptlab.updater")
	Updater updater;
	
	/**
	 * @should return all updates ordered descending by ids
	 */
	@Transactional(readOnly = true)
	public List<Update> getUpdatesInOrder(int first, int max) {
		Criteria update = getSession().createCriteria(Update.class);
		update.addOrder(Order.desc("updateId"));
		update.setFirstResult(first);
		update.setMaxResults(max);
		
		@SuppressWarnings("unchecked")
		List<Update> list = update.list();
		return list;
	}
	
	/**
	 * @should return update with id
	 * @should throw IllegalArgumentException if update does not exist
	 */
	@Transactional(readOnly = true)
	public Update getUpdate(Long id) {
		Update update = (Update) getSession().get(Update.class, id);
		if (update == null) {
			throw new IllegalArgumentException("No update with the given id " + id);
		}
		return update;
	}
	
	@Transactional(readOnly = true)
	public Update getLastUpdate() {
		Criteria update = getSession().createCriteria(Update.class);
		update.addOrder(Order.desc("updateId"));
		update.setMaxResults(1);
		
		return (Update) update.uniqueResult();
	}
	
	@Transactional(readOnly = true)
	public Update getLastSuccessfulUpdate() {
		Criteria update = getSession().createCriteria(Update.class);
		update.add(Restrictions.isNull("errorMessage"));
		update.addOrder(Order.desc("updateId"));
		update.setMaxResults(1);
		
		return (Update) update.uniqueResult();
	}
	
	public void runUpdateNow() {
		assertNoOtherUpdateRunnig();
		scheduler.scheduleNow();
	}
	
	/**
	 * @should throw IllegalStateException if another update is in progress
	 */
	@Transactional
	public void startUpdate(Update update) {
		assertNoOtherUpdateRunnig();
		getSession().save(update);
	}
	
	@Transactional
	public void updateOclDateStarted(Update update, Date oclDateStarted) {
		update.setOclDateStarted(oclDateStarted);
		getSession().save(update);
	}
	
	private void assertNoOtherUpdateRunnig() {
		Update lastUpdate = getLastUpdate();
		if (lastUpdate != null && !lastUpdate.isStopped()) {
			throw new IllegalStateException("Cannot start the update, if there is another update in progress.");
		}
	}
	
	/**
	 * @should throw IllegalArgumentException if not scheduled
	 * @should throw IllegalStateException if trying to stop twice
	 */
	@Transactional
	public void stopUpdate(Update update) {
		if (update.getUpdateId() == null) {
			throw new IllegalArgumentException("Cannot stop the update, if it has not been started.");
		}
		if (update.getLocalDateStopped() != null) {
			throw new IllegalStateException("Cannot stop the update twice.");
		}
		
		update.stop();
		
		getSession().saveOrUpdate(update);
	}
	
	@Transactional(readOnly = true)
	public Item getLastSuccessfulItemByUrl(String url) {
		Criteria criteria = getSession().createCriteria(Item.class);
		criteria.add(Restrictions.eq("url", url));
		criteria.add(Restrictions.not(Restrictions.eq("state", ItemState.ERROR)));
		criteria.addOrder(Order.desc("itemId"));
		criteria.setMaxResults(1);
		return (Item) criteria.uniqueResult();
	}
	
	@Transactional
	public void saveItem(Item item) {
		getSession().saveOrUpdate(item);
	}
	
	@Transactional(readOnly = true)
	public Subscription getSubscription() {
		String url = adminService.getGlobalProperty(OpenConceptLabConstants.GP_SUBSCRIPTION_URL);
		if (url == null) {
			return null;
		}
		Subscription subscription = new Subscription();
		subscription.setUrl(url);
		
		String token = adminService.getGlobalProperty(OpenConceptLabConstants.GP_TOKEN);
		subscription.setToken(token);
		
		String days = adminService.getGlobalProperty(OpenConceptLabConstants.GP_SCHEDULED_DAYS);
		if (!StringUtils.isBlank(days)) {
			subscription.setDays(Integer.valueOf(days));
		}
		
		String time = adminService.getGlobalProperty(OpenConceptLabConstants.GP_SCHEDULED_TIME);
		if (!StringUtils.isBlank(time)) {
			String[] formattedTime = time.split(":");
			if (formattedTime.length != 2) {
				throw new IllegalStateException("Time in the wrong format. Expected 'HH:mm', given: " + time);
			}
			
			subscription.setHours(Integer.valueOf(formattedTime[0]));
			subscription.setMinutes(Integer.valueOf(formattedTime[1]));
		}
		
		return subscription;
	}
	
	private Session getSession() {
		return sessionFactory.getCurrentSession();
	}
	
	@Transactional
	public void saveSubscription(Subscription subscription) {
		GlobalProperty url = adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_SUBSCRIPTION_URL);
		if (url == null) {
			url = new GlobalProperty(OpenConceptLabConstants.GP_SUBSCRIPTION_URL);
		}
		url.setPropertyValue(subscription.getUrl());
		adminService.saveGlobalProperty(url);
		
		GlobalProperty token = adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_TOKEN);
		if (token == null) {
			token = new GlobalProperty(OpenConceptLabConstants.GP_TOKEN);
		}
		token.setPropertyValue(subscription.getToken());
		adminService.saveGlobalProperty(token);
		
		GlobalProperty days = adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_SCHEDULED_DAYS);
		if (days == null) {
			days = new GlobalProperty(OpenConceptLabConstants.GP_SCHEDULED_DAYS);
		}
		
		if (subscription.getDays() != null) {
			days.setPropertyValue(subscription.getDays().toString());
		} else {
			days.setPropertyValue("");
		}
		adminService.saveGlobalProperty(days);
		
		GlobalProperty time = adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_SCHEDULED_TIME);
		if (time == null) {
			time = new GlobalProperty(OpenConceptLabConstants.GP_SCHEDULED_TIME);
		}
		if (subscription.getHours() != null && subscription.getMinutes() != null) {
			time.setPropertyValue(subscription.getHours() + ":" + subscription.getMinutes());
		} else {
			time.setPropertyValue("");
		}
		adminService.saveGlobalProperty(time);
		
		if (!subscription.isManual()) {
			scheduler.schedule(subscription);
		}
	}
	
	@Transactional
	public void unsubscribe() {
		saveSubscription(new Subscription());
		getSession().createQuery("delete from OclItem").executeUpdate();
		getSession().createQuery("delete from OclUpdate").executeUpdate();
	}
	
	@Transactional(readOnly = true)
	public UpdateProgress getUpdateProgress() {
		UpdateProgress updateProgress = new UpdateProgress();
		
		Update lastUpdate = getLastUpdate();
		long time = (new Date().getTime() - lastUpdate.getLocalDateStarted().getTime()) / 1000;
		updateProgress.setTime((int) time);
		
		if (!updater.isDownloaded()) {
			double totalBytesToDownload = updater.getTotalBytesToDownload();
			double progress = 0;
			if (updater.getBytesDownloaded() == 0) {
				//simulate download progress until first bytes are downloaded
				progress = (double) time / (time + 5) * 10.0;
			} else if (updater.getTotalBytesToDownload() == -1) {
				//simulate download progress since total bytes to download are unknown
				progress = 10.0 + ((double) time / (time + 100) * 20.0);
			} else {
				progress = 10.0 + ((double) updater.getBytesDownloaded() / totalBytesToDownload * 20.0);
			}
			updateProgress.setProgress((int) progress);
		} else if (!updater.isProcessed()) {
			double progress = 30;
			if (updater.getTotalBytesToProcess() == -1) {
				progress = 30 + ((double) time / (time + 100) * 70.0);
			} else {
				progress = 30.0 + ((double) updater.getBytesProcessed() / updater.getTotalBytesToProcess() * 70.0);
			}
			updateProgress.setProgress((int) progress);
		} else {
			updateProgress.setProgress(100);
		}
		
		return updateProgress;
	}
	
	public void scheduleUpdate() {
		Subscription subscription = getSubscription();
		if (subscription != null) {
			scheduler.schedule(subscription);
		}
	}
	
	/**
	 * @param update the update to be passed
	 * @param first starting index
	 * @param max maximum limit
	 * @return a list of items
	 */
	public List<Item> getUpdateItems(Update update, int first, int max) {
		Criteria items = getSession().createCriteria(Item.class);
		items.add(Restrictions.eq("update", update));
		items.addOrder(Order.desc("state"));
		items.setFirstResult(first);
		items.setMaxResults(max);
		return items.list();
	}
	
	/**
	 * @param update the update to be passed
	 * @param states set of states passed
	 * @return a count of items
	 */
	public Integer getUpdateItemsCount(Update update, Set<ItemState> states) {
		Criteria items = getSession().createCriteria(Item.class);
		items.add(Restrictions.eq("update", update));
		items.addOrder(Order.desc("state"));
		if (!(states.isEmpty())) {
			items.add(Restrictions.in("state", states));
		}
		return ((Long) items.setProjection(Projections.rowCount()).uniqueResult()).intValue();
	}
	
	/**
	 * @param uuid the uuid to search a concept with
	 * @return true if subscribed else false
	 */
	public Boolean isSubscribedConcept(String uuid) {
		boolean isSubscribed = false;
		Criteria items = getSession().createCriteria(Item.class);
		items.add(Restrictions.eq("type", "Concept"));
		items.add(Restrictions.eq("uuid", uuid));
		if ((Long) (items.setProjection(Projections.rowCount()).uniqueResult()) > 0) {
			isSubscribed = true;
		}
		
		return isSubscribed;
	}
}
