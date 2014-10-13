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

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateService {
	
	@Autowired
	SessionFactory sessionFactory;
		
	/**
	 * @should return all updates ordered descending by ids
	 */
	@Transactional(readOnly = true)
	public List<Update> getUpdatesInOrder() {
		Criteria update = getSession().createCriteria(Update.class);
		update.addOrder(Order.desc("updateId"));
		
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
	
	/**
	 * @should throw IllegalStateException if another update is in progress
	 */
	@Transactional
	public void startUpdate(Update update) {
		Update lastUpdate = getLastUpdate();
		if (lastUpdate != null && !lastUpdate.isStopped()) {
			throw new IllegalStateException("Cannot start the update, if there is another update in progress.");
		}
		
		getSession().save(update);
	}
	
	/**
	 * @should throw IllegalArgumentException if not scheduled
	 * @should throw IllegalStateException if trying to stop twice
	 */
	@Transactional
	public void stopUpdate(Update update) {
		if (update.getUpdateId() == null) {
			throw new IllegalArgumentException("Cannot stop the update, if it has not been scheduled.");
		}
		if (update.getLocalDateStopped() != null) {
			throw new IllegalStateException("Cannot stop the update twice.");
		}
		
		update.stop();
		
		getSession().saveOrUpdate(update);
	}
	
	@Transactional
	public void saveItem(Item item) {
		getSession().saveOrUpdate(item);
	}
	
	@Transactional(readOnly = true)
	public Subscription getSubscription() {
		Subscription subscription = new Subscription();
		return subscription;
	}
	
	private Session getSession() {
		return sessionFactory.getCurrentSession();
	}
}
