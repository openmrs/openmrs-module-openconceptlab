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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
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
import org.openmrs.api.ConceptNameType;

public class ImportServiceImpl implements ImportService {

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
	public List<Import> getImportsInOrder(int first, int max) {
		Criteria update = getSession().createCriteria(Import.class);
		update.addOrder(Order.desc("importId"));
		update.setFirstResult(first);
		update.setMaxResults(max);

		@SuppressWarnings("unchecked")
		List<Import> list = update.list();
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

	@Override
	public List<ConceptName> changeDuplicateConceptNamesToIndexTerms(Concept conceptToImport) {
		List<ConceptName> result = new ArrayList<ConceptName>();

		if (conceptToImport.isRetired()) {
			return Collections.emptyList();
		}

		boolean dbCaseSensitive = adminService.isDatabaseStringComparisonCaseSensitive();
		Iterator<ConceptName> it = conceptToImport.getNames().iterator();
		while(it.hasNext()) {
			ConceptName nameToImport = it.next();

			if (nameToImport.isVoided()) {
				continue;
			}

			if (ConceptNameType.INDEX_TERM.equals(nameToImport.getConceptNameType())) {
				continue; //index terms are never considered duplicates
			}

			if (nameToImport.isLocalePreferred() || nameToImport.isFullySpecifiedName()
					|| nameToImport.equals(nameToImport.getConcept().getName(nameToImport.getLocale()))) {
				Criteria criteria = getSession().createCriteria(ConceptName.class);
				criteria.add(Restrictions.eq("voided", false));
				if (dbCaseSensitive) {
					criteria.add(Restrictions.eq("name", nameToImport.getName()).ignoreCase());
				} else {
					criteria.add(Restrictions.eq("name", nameToImport.getName()));
				}
				criteria.add(Restrictions.or(Restrictions.eq("locale", nameToImport.getLocale()), Restrictions.eq("locale", new Locale(nameToImport
			        .getLocale().getLanguage()))));

				@SuppressWarnings("unchecked")
		        List<ConceptName> conceptNames = criteria.list();

				for (ConceptName conceptName : conceptNames) {
					if (conceptName.getConcept().isRetired()) {
						continue;
					} else if (conceptName.getConcept().getUuid().equals(conceptToImport.getUuid())) {
						continue;
					} else if (conceptName.isLocalePreferred() || conceptName.isFullySpecifiedName()
							|| conceptName.equals(conceptName.getConcept().getName(nameToImport.getLocale()))) {
						//if it is the default name for locale
						nameToImport.setConceptNameType(ConceptNameType.INDEX_TERM);
						nameToImport.setLocalePreferred(false);
						result.add(nameToImport);

						//start again since any previous name to import can be the default name for locale now
						it = conceptToImport.getNames().iterator();
						break;
					}
				}
			}
		}

		return result;
	}

	/**
	 * @should return update with id
	 * @should throw IllegalArgumentException if update does not exist
	 */
	@Override
	public Import getImport(Long id) {
		Import update = (Import) getSession().get(Import.class, id);
		if (update == null) {
			throw new IllegalArgumentException("No update with the given id " + id);
		}
		return update;
	}

	@Override
	public Import getImport(String uuid) {
		Import update = (Import) getSession().createQuery("from OclImport i where i.uuid = :uuid").setString(
				"uuid", uuid).uniqueResult();
		return update;
	}

	@Override
	public Import getLastImport() {
		Criteria update = getSession().createCriteria(Import.class);
		update.addOrder(Order.desc("importId"));
		update.setMaxResults(1);
		return (Import) update.uniqueResult();
	}

	@Override
	public Import getLastSuccessfulSubscriptionImport() {
		Criteria updateCriteria = getSession().createCriteria(Import.class);
		updateCriteria.add(Restrictions.isNull("errorMessage"));
		updateCriteria.add(Restrictions.isNotNull("oclDateStarted"));
		updateCriteria.addOrder(Order.desc("importId"));
		updateCriteria.setMaxResults(1);

		return (Import) updateCriteria.uniqueResult();
	}

	@Override
	public Boolean isLastImportSuccessful(){

		Import lastSuccessfulSubscriptionImport = getLastSuccessfulSubscriptionImport();
		if (lastSuccessfulSubscriptionImport != null) {
			Import lastUpdate = getLastImport();
			return lastSuccessfulSubscriptionImport.equals(lastUpdate);
		}
		else {
			return false;
		}
	}
	
	@Override
	public void ignoreAllErrors(Import anImport) {
		Query query = getSession().createQuery("update OclItem i set i.state = :newState where i.anImport = :anImport and i.state = :oldState");
		query.setParameter("newState", ItemState.IGNORED_ERROR);
		query.setParameter("anImport", anImport);
		query.setParameter("oldState", ItemState.ERROR);
		query.executeUpdate();

		anImport.setErrorMessage(null);
		getSession().saveOrUpdate(anImport);
	}

	@Override
	public void failImport(Import anImport) {
		failImport(anImport, null);
	}

	@Override
	public void failImport(Import update, String errorMessage) {
		update = getImport(update.getImportId());

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
	public void startImport(Import anImport) {
		Import lastImport = getLastImport();
		if (lastImport != null && !lastImport.isStopped()) {
			throw new IllegalStateException("Cannot start the import, if there is another import in progress.");
		}
		getSession().save(anImport);
	}

	@Override
	public void updateOclDateStarted(Import update, Date oclDateStarted) {
		update.setOclDateStarted(oclDateStarted);
		getSession().save(update);
	}

	@Override
	public void updateReleaseVersion(Import anImport, String version) {
		anImport.setReleaseVersion(version);
		getSession().save(anImport);
	}


	/**
	 * @should throw IllegalArgumentException if not scheduled
	 * @should throw IllegalStateException if trying to stop twice
	 */
	@Override
	public void stopImport(Import anImport) {
		if (anImport.getImportId() == null) {
			throw new IllegalArgumentException("Cannot stop the import, if it has not been started.");
		}
		if (anImport.getLocalDateStopped() != null) {
			throw new IllegalStateException("Cannot stop the import twice.");
		}

		anImport = getImport(anImport.getImportId());

		anImport.stop();

		getSession().saveOrUpdate(anImport);
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
			Import update = getImport(item.getAnImport().getImportId());
			item.setAnImport(update); //replace with attached object

			saveItem(item);
        }
    }

	@Override
	public Item getItem(String uuid) {
		Item item = (Item) getSession().createQuery("from OclItem i where i.uuid = :uuid").setString(
				"uuid", uuid).uniqueResult();
		return item;
	}

	@Override
	public Subscription getSubscription() {
		String url = adminService.getGlobalProperty(OpenConceptLabConstants.GP_SUBSCRIPTION_URL);
		if (url == null) {
			return null;
		}
		Subscription subscription = new Subscription();
		subscription.setUrl(url);

		String uuid = adminService.getGlobalProperty(OpenConceptLabConstants.GP_SUBSCRIPTION_UUID);
		subscription.setUuid(uuid);

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
		GlobalProperty uuid= adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_SUBSCRIPTION_UUID);
		if (uuid == null) {
			uuid = new GlobalProperty(OpenConceptLabConstants.GP_SUBSCRIPTION_UUID);
		}
		uuid.setPropertyValue(subscription.getUuid());
		adminService.saveGlobalProperty(uuid);

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
		getSession().createQuery("delete from OclImport").executeUpdate();
	}

	/**
	 * @param anImport the update to be passed
	 * @param first starting index
	 * @param max maximum limit
	 * @return a list of items
	 */
	@SuppressWarnings("unchecked")
    @Override
    public List<Item> getImportItems(Import anImport, int first, int max, Set<ItemState> states) {
		Criteria items = getSession().createCriteria(Item.class);
		items.add(Restrictions.eq("anImport", anImport));
		if (!states.isEmpty()) {
			items.add(Restrictions.in("state", states));
		}
		items.addOrder(Order.desc("state"));
		items.setFirstResult(first);
		items.setMaxResults(max);

		return items.list();
	}

	/**
	 * @param anImport the update to be passed
	 * @param states set of states passed
	 * @return a count of items
	 */
	@Override
    public Integer getImportItemsCount(Import anImport, Set<ItemState> states) {
		Criteria items = getSession().createCriteria(Item.class);
		items.add(Restrictions.eq("anImport", anImport));
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

	@Override
	public void updateSubscriptionUrl(Import anImport, String url) {
		anImport.setSubscriptionUrl(url);
		getSession().saveOrUpdate(anImport);
	}
}
