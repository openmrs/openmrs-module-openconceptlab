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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.hibernate.Session;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.api.db.hibernate.HibernateUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;

public class ImportServiceImpl implements ImportService {

	DbSessionFactory sessionFactory;

	AdministrationService adminService;

	ConceptService conceptService;
	
	OclConceptService oclConceptService;

	public void setSessionFactory(DbSessionFactory sessionFactory) {
	    this.sessionFactory = sessionFactory;
    }

    public void setAdminService(AdministrationService adminService) {
	    this.adminService = adminService;
    }

	public void setConceptService(ConceptService conceptService) {
		this.conceptService = conceptService;
	}
	
	public void setOclConceptService(OclConceptService oclConceptService) {
		this.oclConceptService = oclConceptService;
	}

	/**
	 * @should return all updates ordered descending by ids
	 */
	@Override
	public List<Import> getImportsInOrder(int first, int max) {
		Query<Import> update = getSession().createQuery("from OclImport i order by i.importId desc", Import.class);
		update.setFirstResult(first);
		update.setMaxResults(max);

		List<Import> list = update.list();
		return list;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Import> getInProgressImports() {
		return getSession().createQuery("from OclImport i where i.localDateStopped is null order by i.importId desc", Import.class)
				.list();
	}

	@Override
	public List<Concept> getConceptsByName(String name, Locale locale) {
		String nameRestriction = adminService.isDatabaseStringComparisonCaseSensitive() ? "lower(cn.name) = lower(:name)"
				: "cn.name = :name";
		Query<ConceptName> criteria = getSession().createQuery(
				"from ConceptName cn where cn.voided = false and " + nameRestriction + " and cn.locale = :locale", ConceptName.class);
		criteria.setParameter("name", name);
		criteria.setParameter("locale", locale);

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
				String nameRestriction = dbCaseSensitive ? "lower(cn.name) = lower(:name)" : "cn.name = :name";
				Query<ConceptName> criteria = getSession().createQuery("from ConceptName cn where cn.voided = false and "
						+ nameRestriction + " and (cn.locale = :locale or cn.locale = :languageLocale)", ConceptName.class);
				criteria.setParameter("name", nameToImport.getName());
				criteria.setParameter("locale", nameToImport.getLocale());
				criteria.setParameter("languageLocale", new Locale(nameToImport.getLocale().getLanguage()));

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
		Import update = getSession().get(Import.class, id);
		if (update == null) {
			throw new IllegalArgumentException("No update with the given id " + id);
		}
		return update;
	}

	@Override
	public Import getImport(String uuid) {
		Import update = getSession().createQuery("from OclImport i where i.uuid = :uuid", Import.class).setParameter(
				"uuid", uuid).uniqueResult();
		return update;
	}

	@Override
	public Import getLastImport() {
		Query<Import> update = getSession().createQuery("from OclImport i order by i.importId desc", Import.class);
		update.setMaxResults(1);
		return update.uniqueResult();
	}

	@Override
	public Import getLastSuccessfulSubscriptionImport() {
		Query<Import> updateCriteria = getSession().createQuery(
				"from OclImport i where i.errorMessage is null and i.oclDateStarted is not null order by i.importId desc",
				Import.class);
		updateCriteria.setMaxResults(1);

		return updateCriteria.uniqueResult();
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
		MutationQuery query = getSession().createMutationQuery("update OclItem i set i.state = :newState where i.anImport = :anImport and i.state = :oldState");
		query.setParameter("newState", ItemState.IGNORED_ERROR);
		query.setParameter("anImport", anImport);
		query.setParameter("oldState", ItemState.ERROR);
		query.executeUpdate();

		anImport.setErrorMessage(null);
		HibernateUtil.saveOrUpdate(getSession(), anImport);
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
		HibernateUtil.saveOrUpdate(getSession(), update);
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
		getSession().persist(anImport);
	}

	@Override
	public void updateOclDateStarted(Import update, Date oclDateStarted) {
		update.setOclDateStarted(oclDateStarted);
		HibernateUtil.saveOrUpdate(getSession(), update);
	}

	@Override
	public void updateReleaseVersion(Import anImport, String version) {
		anImport.setReleaseVersion(version);
		HibernateUtil.saveOrUpdate(getSession(), anImport);
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

		HibernateUtil.saveOrUpdate(getSession(), anImport);
	}

	@Override
	public Item getLastSuccessfulItemByUrl(String url) {
		return getLastSuccessfulItemByUrl(url, new CacheService(conceptService, oclConceptService));
	}

	@Override
	public Item getLastSuccessfulItemByUrl(String url, CacheService cacheService) {
		//hashedUrl is indexed to speed up the search
		Query<Item> criteria = getSession().createQuery(
				"from OclItem i where i.hashedUrl = :hashedUrl and i.url = :url and i.state <> :state order by i.itemId desc",
				Item.class);
		criteria.setParameter("hashedUrl", Item.hashUrl(url));
		criteria.setParameter("url", url);
		criteria.setParameter("state", ItemState.ERROR);
		criteria.setMaxResults(1);

		Item item = criteria.uniqueResult();
		if (item != null) {
			switch (item.getType()) {
				case MAPPING:
					ConceptMap map = cacheService.getConceptMapByUuid(item.getUuid(), this);
					if (map == null) {
						return null;
					}
					break;
				case CONCEPT:
					Concept concept = cacheService.getConceptByUuid(item.getUuid());
					if (concept == null) {
						return null;
					}
					break;
				default:
					throw new RuntimeException("Item with UUID=" + item.getUuid() + " couldn't be recognized as Concept or Mapping");
			}
		}
		return item;
	}

	@Override
	public void saveItem(Item item) {
		HibernateUtil.saveOrUpdate(getSession(), item);
	}

	@Override
	public void saveItems(Iterable<? extends Item> items) {
		Import attachedImport = null;
		for (Item item : items) {
			// Fetch the Import once and reuse for all items in the batch
			if (attachedImport == null) {
				attachedImport = getImport(item.getAnImport().getImportId());
			}
			item.setAnImport(attachedImport);

			saveItem(item);
		}
	}

	@Override
	public Item getItem(String uuid) {
		Item item = getSession().createQuery("from OclItem i where i.uuid = :uuid", Item.class).setParameter(
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
		subscription.setUrl(StringEscapeUtils.unescapeHtml4(url));

		String uuid = adminService.getGlobalProperty(OpenConceptLabConstants.GP_SUBSCRIPTION_UUID);
		subscription.setUuid(uuid);

		String token = adminService.getGlobalProperty(OpenConceptLabConstants.GP_TOKEN);
		subscription.setToken(StringEscapeUtils.unescapeHtml4(token));

		String validationType = adminService.getGlobalProperty(OpenConceptLabConstants.GP_VALIDATION_TYPE);
		if (StringUtils.isNotBlank(validationType)) {
			subscription.setValidationType(ValidationType.valueOf(validationType));
		}

		String days = adminService.getGlobalProperty(OpenConceptLabConstants.GP_SCHEDULED_DAYS);
		if (!StringUtils.isBlank(days)) {
			subscription.setDays(Integer.valueOf(days));
		}

		String subscribedToSnapshot = adminService.getGlobalProperty(OpenConceptLabConstants.GP_SUBSCRIBED_TO_SNAPSHOT);
		subscription.setSubscribedToSnapshot(Boolean.valueOf(subscribedToSnapshot));

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
		return sessionFactory.getHibernateSessionFactory().getCurrentSession();
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
		url.setPropertyValue(prependApiIfAbsent(subscription.getUrl()));
		adminService.saveGlobalProperty(url);

		GlobalProperty token = adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_TOKEN);
		if (token == null) {
			token = new GlobalProperty(OpenConceptLabConstants.GP_TOKEN);
		}
		token.setPropertyValue(subscription.getToken());
		adminService.saveGlobalProperty(token);

		GlobalProperty validationType = adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_VALIDATION_TYPE);
		if (validationType == null) {
			validationType = new GlobalProperty(OpenConceptLabConstants.GP_VALIDATION_TYPE);
		}
		validationType.setPropertyValue(subscription.getValidationType().name());
		adminService.saveGlobalProperty(validationType);

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

		GlobalProperty subscribedToSnapshot = adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_SUBSCRIBED_TO_SNAPSHOT);
		if (subscribedToSnapshot == null) {
			subscribedToSnapshot = new GlobalProperty(OpenConceptLabConstants.GP_SUBSCRIBED_TO_SNAPSHOT);
		}
		subscribedToSnapshot.setPropertyValue(String.valueOf(subscription.isSubscribedToSnapshot()));
		adminService.saveGlobalProperty(subscribedToSnapshot);

	}

	private String prependApiIfAbsent(String stringUrl) {
		if (StringUtils.isNotBlank(stringUrl)) {
			try {
				URL url = new URL(stringUrl);
				String host = url.getHost();
				if (!host.startsWith("api.")) {
					return url.toString().replace(host, "api." + host);
				}
				return url.toString();
			} catch (MalformedURLException e) {
				throw new IllegalStateException("Wrong url address");
			}
		} else {
			return stringUrl;
		}
	}

	@Override
	public void unsubscribe() {
		saveSubscription(new Subscription());
		getSession().createMutationQuery("delete from OclItem").executeUpdate();
		getSession().createMutationQuery("delete from OclImport").executeUpdate();
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
		Query<Item> items = getSession().createQuery("from OclItem i where i.anImport = :anImport"
				+ (states.isEmpty() ? "" : " and i.state in (:states)") + " order by i.state desc", Item.class);
		items.setParameter("anImport", anImport);
		if (!states.isEmpty()) {
			items.setParameterList("states", states);
		}
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
		Query<Long> items = getSession().createQuery("select count(*) from OclItem i where i.anImport = :anImport"
				+ (states.isEmpty() ? "" : " and i.state in (:states)"), Long.class);
		items.setParameter("anImport", anImport);
		if (!(states.isEmpty())) {
			items.setParameterList("states", states);
		}
		return items.uniqueResult().intValue();
	}

	/**
	 * @param uuid the uuid to search a concept with
	 * @return true if subscribed else false
	 */
	@Override
    public Boolean isSubscribedConcept(String uuid) {
		boolean isSubscribed = false;
		Query<Long> items = getSession().createQuery("select count(*) from OclItem i where i.type = :type and i.uuid = :uuid",
				Long.class);
		items.setParameter("type", ItemType.CONCEPT);
		items.setParameter("uuid", uuid);
		if (items.uniqueResult() > 0) {
			isSubscribed = true;
		}

		return isSubscribed;
	}

	@Override
	public ConceptMap getConceptMapByUuid(String uuid) {
		return getSession().createQuery("from ConceptMap cm where cm.uuid = :uuid", ConceptMap.class).setParameter("uuid", uuid)
				.uniqueResult();
	}

	@Override
	public Concept updateConceptWithoutValidation(Concept concept) {
		HibernateUtil.saveOrUpdate(getSession(), concept);
		return concept;
	}

	@Override
	public ConceptReferenceTerm updateConceptReferenceTermWithoutValidation(ConceptReferenceTerm term) {
		HibernateUtil.saveOrUpdate(getSession(), term);
		return term;
    }

	@Override
	public void updateSubscriptionUrl(Import anImport, String url) {
		anImport.setSubscriptionUrl(url);
		HibernateUtil.saveOrUpdate(getSession(), anImport);
	}

	@Override
	public <T> T runInTransaction(Callable<T> callable) throws Exception {
		return callable.call();
	}

	@Override
	public void flushAndClearSession() {
		Session session = getSession();
		session.flush();
		session.clear();
	}

}
