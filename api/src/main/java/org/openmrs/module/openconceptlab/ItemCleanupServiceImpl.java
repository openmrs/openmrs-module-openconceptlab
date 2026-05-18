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
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.api.AdministrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemCleanupServiceImpl implements ItemCleanupService {

	private static final Logger log = LoggerFactory.getLogger(ItemCleanupServiceImpl.class);

	private static final int DELETE_BATCH_SIZE = 1000;

	private static final String RETENTION_TYPE_RUNS = "RUNS";

	private static final String RETENTION_TYPE_DAYS = "DAYS";

	SessionFactory sessionFactory;

	AdministrationService adminService;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setAdminService(AdministrationService adminService) {
		this.adminService = adminService;
	}

	@Override
	@Transactional
	public int runCleanup() {
		String retentionType = adminService.getGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETENTION_TYPE);
		if (StringUtils.isBlank(retentionType)) {
			log.debug("Item cleanup is disabled (no retention policy configured)");
			return 0;
		}

		retentionType = retentionType.trim().toUpperCase();
		if (!RETENTION_TYPE_RUNS.equals(retentionType) && !RETENTION_TYPE_DAYS.equals(retentionType)) {
			log.warn("Unknown cleanup retention type: '{}'. Expected RUNS or DAYS.", retentionType);
			return 0;
		}

		String retentionValueStr;
		if (RETENTION_TYPE_RUNS.equals(retentionType)) {
			retentionValueStr = adminService.getGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETAIN_IMPORTS);
		} else {
			retentionValueStr = adminService.getGlobalProperty(OpenConceptLabConstants.GP_CLEANUP_RETAIN_DAYS);
		}

		int retentionValue;
		try {
			retentionValue = Integer.parseInt(StringUtils.trimToEmpty(retentionValueStr));
		}
		catch (NumberFormatException e) {
			log.warn("Invalid cleanup retention value: '{}'. Skipping cleanup.", retentionValueStr);
			return 0;
		}

		if (retentionValue <= 0) {
			log.warn("Cleanup retention value must be positive, got: {}. Skipping cleanup.", retentionValue);
			return 0;
		}

		log.info("Starting item cleanup with policy: {} = {}", retentionType, retentionValue);

		Set<Long> protectedImportIds = getProtectedImportIds(retentionType, retentionValue);
		Set<Long> preservedItemIds = getLatestItemIdPerUrl();

		int itemsDeleted = deleteEligibleItems(protectedImportIds, preservedItemIds);

		// Ensure item deletions are visible to the orphan detection query
		getSession().flush();
		getSession().clear();

		int importsDeleted = deleteOrphanedImports();

		log.info("Cleanup complete: {} items deleted, {} orphaned imports deleted", itemsDeleted, importsDeleted);
		return itemsDeleted;
	}

	/**
	 * Builds the set of import IDs that must not have their items deleted.
	 * This always includes in-progress imports and the most recent successful import,
	 * plus imports covered by the retention policy. An import is considered successful
	 * if it has at least one non-ERROR item (i.e., it actually processed something).
	 */
	@SuppressWarnings("unchecked")
	private Set<Long> getProtectedImportIds(String retentionType, int retentionValue) {
		Session session = getSession();
		Set<Long> protectedIds = new HashSet<>();

		// An import is "successful" if it processed at least one item without error.
		// This distinguishes crashed imports (no items or only ERROR items) from
		// partially successful ones (some errors but also real work done).
		String hasNonErrorItems =
				"EXISTS (SELECT 1 FROM OclItem item WHERE item.anImport = i " +
				"AND (item.state IS NULL OR item.state <> :errorState))";

		// Always protect in-progress imports
		List<Long> inProgressIds = session.createQuery(
				"SELECT i.importId FROM OclImport i WHERE i.localDateStopped IS NULL")
				.list();
		protectedIds.addAll(inProgressIds);

		if (RETENTION_TYPE_RUNS.equals(retentionType)) {
			// Protect the N most recent successful imports.
			// This implicitly includes the most recent successful import.
			Query runsQuery = session.createQuery(
					"SELECT i.importId FROM OclImport i " +
					"WHERE i.localDateStopped IS NOT NULL " +
					"AND " + hasNonErrorItems + " " +
					"ORDER BY i.importId DESC");
			runsQuery.setParameter("errorState", ItemState.ERROR);
			runsQuery.setMaxResults(retentionValue);
			protectedIds.addAll(runsQuery.list());
		} else {
			// Protect imports completed within the last N days
			Calendar cutoff = Calendar.getInstance();
			cutoff.add(Calendar.DAY_OF_YEAR, -retentionValue);
			List<Long> dayIds = session.createQuery(
					"SELECT i.importId FROM OclImport i " +
					"WHERE i.localDateStopped IS NOT NULL " +
					"AND i.localDateStopped >= :cutoffDate")
					.setParameter("cutoffDate", cutoff.getTime())
					.list();
			protectedIds.addAll(dayIds);

			// Always protect the most recent successful import even if it's older than the cutoff
			Query latestQuery = session.createQuery(
					"SELECT i.importId FROM OclImport i " +
					"WHERE i.localDateStopped IS NOT NULL " +
					"AND " + hasNonErrorItems + " " +
					"ORDER BY i.importId DESC");
			latestQuery.setParameter("errorState", ItemState.ERROR);
			latestQuery.setMaxResults(1);
			Long latestId = (Long) latestQuery.uniqueResult();
			if (latestId != null) {
				protectedIds.add(latestId);
			}
		}

		return protectedIds;
	}

	/**
	 * Returns the item ID of the most recent non-error item for each unique URL.
	 * These items are preserved regardless of the retention policy to ensure
	 * at least one successful record exists for every imported concept/mapping
	 * that has one.
	 */
	@SuppressWarnings("unchecked")
	private Set<Long> getLatestItemIdPerUrl() {
		// MAX(i.itemId) gives the most recently created item per URL because itemId
		// is an auto-increment primary key. hashedUrl is indexed for efficient GROUP BY.
		List<Long> ids = getSession().createQuery(
				"SELECT MAX(i.itemId) FROM OclItem i " +
				"WHERE (i.state IS NULL OR i.state <> :errorState) " +
				"GROUP BY i.hashedUrl")
				.setParameter("errorState", ItemState.ERROR)
				.list();

		return new HashSet<>(ids);
	}

	/**
	 * Deletes items that are not in protected imports and not the latest item for their URL.
	 * Uses a two-phase approach (select IDs then delete by ID) to avoid MySQL limitations
	 * with subqueries on the same table in DELETE statements.
	 * Fetches eligible IDs in pages using a cursor (itemId ordering) and deletes in batches
	 * to limit IN-clause parameter list sizes and periodically clear the Hibernate session cache.
	 */
	private int deleteEligibleItems(Set<Long> protectedImportIds, Set<Long> preservedItemIds) {
		if (protectedImportIds.isEmpty()) {
			// No protected imports means either no imports exist (fresh install) or we
			// can't safely determine what to keep — either way, nothing to delete
			log.debug("No protected imports found. Skipping item deletion.");
			return 0;
		}

		int totalDeleted = 0;
		long lastSeenId = 0;

		while (true) {
			@SuppressWarnings("unchecked")
			List<Long> batch = getSession().createQuery(
					"SELECT i.itemId FROM OclItem i " +
					"WHERE i.anImport.importId NOT IN (:protectedImports) " +
					"AND i.itemId > :lastSeenId " +
					"ORDER BY i.itemId")
					.setParameterList("protectedImports", protectedImportIds)
					.setParameter("lastSeenId", lastSeenId)
					.setMaxResults(DELETE_BATCH_SIZE)
					.list();

			if (batch.isEmpty()) {
				break;
			}

			lastSeenId = batch.get(batch.size() - 1);

			// Remove preserved items (latest per URL) in Java to avoid very large NOT IN clauses
			batch = new ArrayList<>(batch);
			batch.removeAll(preservedItemIds);

			if (batch.isEmpty()) {
				continue;
			}

			int deleted = getSession().createQuery(
					"DELETE FROM OclItem i WHERE i.itemId IN (:ids)")
					.setParameterList("ids", batch)
					.executeUpdate();
			totalDeleted += deleted;

			getSession().flush();
			getSession().clear();
		}

		return totalDeleted;
	}

	/**
	 * Deletes completed Import records that have no remaining Items.
	 * Only deletes imports that are already stopped (never in-progress ones).
	 */
	@SuppressWarnings("unchecked")
	private int deleteOrphanedImports() {
		List<Long> orphanIds = getSession().createQuery(
				"SELECT i.importId FROM OclImport i " +
				"WHERE i.localDateStopped IS NOT NULL " +
				"AND NOT EXISTS (" +
				"  SELECT 1 FROM OclItem item WHERE item.anImport = i" +
				")")
				.list();

		if (orphanIds.isEmpty()) {
			return 0;
		}

		return getSession().createQuery(
				"DELETE FROM OclImport i WHERE i.importId IN (:ids)")
				.setParameterList("ids", orphanIds)
				.executeUpdate();
	}

	private Session getSession() {
		return sessionFactory.getCurrentSession();
	}
}
