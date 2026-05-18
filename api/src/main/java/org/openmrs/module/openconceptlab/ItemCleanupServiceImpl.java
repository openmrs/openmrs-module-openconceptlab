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
import org.hibernate.query.Query;
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
	 * Builds the set of import IDs whose items must not be deleted. Always includes:
	 * <ul>
	 *   <li>in-progress imports (never touched while running)</li>
	 *   <li>imports covered by the configured retention policy — the N most recent stopped
	 *       imports under RUNS (regardless of success/failure), or imports stopped within
	 *       the cutoff under DAYS</li>
	 *   <li>the most recent successful import (an import with at least one non-ERROR item),
	 *       pinned so {@code getLastSuccessfulSubscriptionImport()} keeps returning a usable
	 *       row even if the head of history is all failures</li>
	 * </ul>
	 */
	private Set<Long> getProtectedImportIds(String retentionType, int retentionValue) {
		Session session = getSession();

        // "Successful" = at least one non-ERROR item. This is only used to pin the most
		// recent good import; retention itself counts every stopped import regardless of
		// state. Without the pin, a string of failures could push every successful import
		// out of the retain window, forcing a full refetch on the next incremental update.
		String hasNonErrorItems =
				"EXISTS (SELECT 1 FROM OclItem item WHERE item.anImport = i " +
				"AND (item.state IS NULL OR item.state <> :errorState))";

		// Always protect in-progress imports
		List<Long> inProgressIds = session.createQuery(
				"SELECT i.importId FROM OclImport i WHERE i.localDateStopped IS NULL", Long.class)
				.list();
        Set<Long> protectedIds = new HashSet<>(inProgressIds);

		if (RETENTION_TYPE_RUNS.equals(retentionType)) {
			// Protect the N most recent stopped imports regardless of state
			List<Long> recentIds = session.createQuery(
					"SELECT i.importId FROM OclImport i " +
					"WHERE i.localDateStopped IS NOT NULL " +
					"ORDER BY i.importId DESC", Long.class)
					.setMaxResults(retentionValue)
					.list();
			protectedIds.addAll(recentIds);

			// Additionally, pin the most recent successful import (may be older than the
			// retention window) so incremental updates can still resume from it.
			Query<Long> latestSuccessful = session.createQuery(
					"SELECT i.importId FROM OclImport i " +
					"WHERE i.localDateStopped IS NOT NULL " +
					"AND " + hasNonErrorItems + " " +
					"ORDER BY i.importId DESC", Long.class);
			latestSuccessful.setParameter("errorState", ItemState.ERROR);
			latestSuccessful.setMaxResults(1);
			Long latestSuccessfulId = latestSuccessful.uniqueResult();
			if (latestSuccessfulId != null) {
				protectedIds.add(latestSuccessfulId);
			}
		} else {
			// Protect imports completed within the last N days
			Calendar cutoff = Calendar.getInstance();
			cutoff.add(Calendar.DAY_OF_YEAR, -retentionValue);
			List<Long> dayIds = session.createQuery(
					"SELECT i.importId FROM OclImport i " +
					"WHERE i.localDateStopped IS NOT NULL " +
					"AND i.localDateStopped >= :cutoffDate", Long.class)
					.setParameter("cutoffDate", cutoff.getTime())
					.list();
			protectedIds.addAll(dayIds);

			// Always protect the most recent successful import even if it's older than the cutoff
			Query<Long> latestQuery = session.createQuery(
					"SELECT i.importId FROM OclImport i " +
					"WHERE i.localDateStopped IS NOT NULL " +
					"AND " + hasNonErrorItems + " " +
					"ORDER BY i.importId DESC", Long.class);
			latestQuery.setParameter("errorState", ItemState.ERROR);
			latestQuery.setMaxResults(1);
			Long latestId = latestQuery.uniqueResult();
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
	private Set<Long> getLatestItemIdPerUrl() {
		// MAX(i.itemId) gives the most recently created item per URL because itemId
		// is an auto-increment primary key. hashedUrl is indexed for efficient GROUP BY.
		List<Long> ids = getSession().createQuery(
				"SELECT MAX(i.itemId) FROM OclItem i " +
				"WHERE (i.state IS NULL OR i.state <> :errorState) " +
				"GROUP BY i.hashedUrl", Long.class)
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
		int totalDeleted = 0;
		long lastSeenId = 0;

		while (true) {
			// An empty protected set is a legitimate state — e.g. a fresh install with no
			// imports, or DAYS retention where every import is older than the cutoff and
			// no successful import exists to pin. We still proceed so orphaned items get
			// cleaned up rather than accumulating forever.
			List<Long> batch = fetchEligibleBatch(protectedImportIds, lastSeenId);

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

	private List<Long> fetchEligibleBatch(Set<Long> protectedImportIds, long lastSeenId) {
		if (protectedImportIds.isEmpty()) {
			return getSession().createQuery(
					"SELECT i.itemId FROM OclItem i " +
					"WHERE i.itemId > :lastSeenId " +
					"ORDER BY i.itemId", Long.class)
					.setParameter("lastSeenId", lastSeenId)
					.setMaxResults(DELETE_BATCH_SIZE)
					.list();
		}
		return getSession().createQuery(
				"SELECT i.itemId FROM OclItem i " +
				"WHERE i.anImport.importId NOT IN (:protectedImports) " +
				"AND i.itemId > :lastSeenId " +
				"ORDER BY i.itemId", Long.class)
				.setParameterList("protectedImports", protectedImportIds)
				.setParameter("lastSeenId", lastSeenId)
				.setMaxResults(DELETE_BATCH_SIZE)
				.list();
	}

	/**
	 * Deletes completed Import records that have no remaining Items.
	 * Only deletes imports that are already stopped (never in-progress ones).
	 */
	private int deleteOrphanedImports() {
		List<Long> orphanIds = getSession().createQuery(
				"SELECT i.importId FROM OclImport i " +
				"WHERE i.localDateStopped IS NOT NULL " +
				"AND NOT EXISTS (" +
				"  SELECT 1 FROM OclItem item WHERE item.anImport = i" +
				")", Long.class)
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
