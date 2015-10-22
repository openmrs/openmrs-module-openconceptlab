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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;

public class UpdateServiceImpl implements UpdateService {
	
	SessionFactory sessionFactory;
	
	AdministrationService adminService;
	
    public void setSessionFactory(SessionFactory sessionFactory) {
	    this.sessionFactory = sessionFactory;
    }
    
    public void setAdminService(AdministrationService adminService) {
	    this.adminService = adminService;
    }
	
	/**
	 * @should return all updates ordered descending by ids
	 */
	@Override
	public List<Update> getUpdatesInOrder(int first, int max) {
		Criteria update = getSession().createCriteria(Update.class);
		update.addOrder(Order.desc("updateId"));
		update.setFirstResult(first);
		update.setMaxResults(max);
		
		@SuppressWarnings("unchecked")
		List<Update> list = update.list();
		return list;
	}
	
	@Override
	public List<Concept> getConceptsByName(String name, Locale locale) {
		Criteria criteria = getSession().createCriteria(ConceptName.class);
		criteria.add(Restrictions.eq("voided", false));
		if (adminService.isDatabaseStringComparisonCaseSensitive()) {
			criteria.add(Restrictions.eq("name", name).ignoreCase());
		} else {
			criteria.add(Restrictions.eq("name", name));
		}
		criteria.add(Restrictions.eq("locale", locale));
		
		@SuppressWarnings("unchecked")
        List<ConceptName> conceptNames = criteria.list();
		
		Set<Concept> concepts = new LinkedHashSet<Concept>();
		for (ConceptName conceptName : conceptNames) {
	        concepts.add(conceptName.getConcept());
        }
		return new ArrayList<Concept>(concepts);
	}
	
	/**
	 * @should return update with id
	 * @should throw IllegalArgumentException if update does not exist
	 */
	@Override
	public Update getUpdate(Long id) {
		Update update = (Update) getSession().get(Update.class, id);
		if (update == null) {
			throw new IllegalArgumentException("No update with the given id " + id);
		}
		return update;
	}
	
	@Override
	public Update getLastUpdate() {
		Criteria update = getSession().createCriteria(Update.class);
		update.addOrder(Order.desc("updateId"));
		update.setMaxResults(1);
		return (Update) update.uniqueResult();
	}
	
	@Override
	public Update getLastSuccessfulSubscriptionUpdate() {
		Criteria updateCriteria = getSession().createCriteria(Update.class);
		updateCriteria.add(Restrictions.isNull("errorMessage"));
		updateCriteria.add(Restrictions.isNotNull("oclDateStarted"));
		updateCriteria.addOrder(Order.desc("updateId"));
		updateCriteria.setMaxResults(1);
		
		return (Update) updateCriteria.uniqueResult();
	}
	
	@Override
	public void ignoreAllErrors(Update update) {
		Query query = getSession().createQuery("update OclItem i set i.state = :newState where i.update = :update and i.state = :oldState");
		query.setParameter("newState", ItemState.IGNORED_ERROR);
		query.setParameter("update", update);
		query.setParameter("oldState", ItemState.ERROR);
		query.executeUpdate();
		
		update.setErrorMessage(null);
		getSession().saveOrUpdate(update);
	}
	
	@Override
	public void failUpdate(Update update) {
		failUpdate(update, null);
	}
	
	@Override
	public void failUpdate(Update update, String errorMessage) {
		update = getUpdate(update.getUpdateId());
		
		if (!StringUtils.isBlank(errorMessage)) {
			update.setErrorMessage(errorMessage);
		} else {
			update.setErrorMessage("Errors found");
		}
		getSession().saveOrUpdate(update);
	}
	
	/**
	 * @should throw IllegalStateException if another update is in progress
	 */
	@Override
	public void startUpdate(Update update) {
		Update lastUpdate = getLastUpdate();
		if (lastUpdate != null && !lastUpdate.isStopped()) {
			throw new IllegalStateException("Cannot start the update, if there is another update in progress.");
		}
		getSession().save(update);
	}
	
	@Override
	public void updateOclDateStarted(Update update, Date oclDateStarted) {
		update.setOclDateStarted(oclDateStarted);
		getSession().save(update);
	}
	
	
	/**
	 * @should throw IllegalArgumentException if not scheduled
	 * @should throw IllegalStateException if trying to stop twice
	 */
	@Override
	public void stopUpdate(Update update) {
		if (update.getUpdateId() == null) {
			throw new IllegalArgumentException("Cannot stop the update, if it has not been started.");
		}
		if (update.getLocalDateStopped() != null) {
			throw new IllegalStateException("Cannot stop the update twice.");
		}
		
		update = getUpdate(update.getUpdateId());
		
		update.stop();
		
		getSession().saveOrUpdate(update);
	}
	
	@Override
	public Item getLastSuccessfulItemByUrl(String url) {
		Criteria criteria = getSession().createCriteria(Item.class);
		criteria.add(Restrictions.eq("hashedUrl", Item.hashUrl(url)));
		criteria.add(Restrictions.not(Restrictions.eq("state", ItemState.ERROR)));
		criteria.addOrder(Order.desc("itemId"));
		criteria.setMaxResults(1);
		return (Item) criteria.uniqueResult();
	}
	
	@Override
	public void saveItem(Item item) {
		getSession().saveOrUpdate(item);
	}
	
	@Override
	public void saveItems(Iterable<? extends Item> items) {
		for (Item item : items) {			
			Update update = getUpdate(item.getUpdate().getUpdateId()); 
			item.setUpdate(update); //replace with attached object
			
			saveItem(item);
        }
    }
	
	@Override
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
	
	@Override
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
	}
	
	@Override
	public void unsubscribe() {
		saveSubscription(new Subscription());
		getSession().createQuery("delete from OclItem").executeUpdate();
		getSession().createQuery("delete from OclUpdate").executeUpdate();
	}   
	
	/**
	 * @param update the update to be passed
	 * @param first starting index
	 * @param max maximum limit
	 * @return a list of items
	 */
	@SuppressWarnings("unchecked")
    @Override
    public List<Item> getUpdateItems(Update update, int first, int max, Set<ItemState> states) {
		Criteria items = getSession().createCriteria(Item.class);
		items.add(Restrictions.eq("update", update));
		if (!states.isEmpty()) {
			items.add(Restrictions.in("state", states));
		}
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
	@Override
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
	@Override
    public Boolean isSubscribedConcept(String uuid) {
		boolean isSubscribed = false;
		Criteria items = getSession().createCriteria(Item.class);
		items.add(Restrictions.eq("type", ItemType.CONCEPT));
		items.add(Restrictions.eq("uuid", uuid));
		if ((Long) (items.setProjection(Projections.rowCount()).uniqueResult()) > 0) {
			isSubscribed = true;
		}
		
		return isSubscribed;
	}
	
	@Override
	public ConceptMap getConceptMapByUuid(String uuid) {
		Criteria criteria = getSession().createCriteria(ConceptMap.class);
		criteria.add(Restrictions.eq("uuid", uuid));
		return (ConceptMap) criteria.uniqueResult();
	}
	
	@Override
	public Concept updateConceptWithoutValidation(Concept concept) {
		getSession().saveOrUpdate(concept);
		return concept;
	}

	@Override
	public ConceptReferenceTerm updateConceptReferenceTermWithoutValidation(ConceptReferenceTerm term) {
		getSession().saveOrUpdate(term);
		return term;
    }
}
