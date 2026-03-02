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
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory cache layer for the OCL import pipeline. A new instance is created per import run
 * in Importer.processInput() and is garbage collected when the import completes.
 * <p>
 * Most entity caches (concepts, conceptMaps, etc.) are cleared on each flush/clear cycle via
 * {@link #clearCache()}. The item URL caches ({@code itemsByUrl}, {@code checkedItemUrls}) grow
 * monotonically for the lifetime of the import since they must persist across batches and phases.
 * For typical imports (~15K items), this is a few MB. For very large imports (hundreds of thousands
 * of items), memory usage should be monitored.
 */
public class CacheService {

	ConceptService conceptService;

	OclConceptService oclConceptService;

	// Track whether various caches have been pre-loaded
	private boolean conceptSourcesPreloaded = false;
	private boolean conceptDatatypesPreloaded = false;
	private boolean conceptClassesPreloaded = false;
	private boolean conceptMapTypesPreloaded = false;

	public CacheService(ConceptService conceptService, OclConceptService oclConceptService) {
		this.conceptService = conceptService;
		this.oclConceptService = oclConceptService;
	}

	Map<String, ConceptDatatype> conceptDatatypes = new HashMap<>();

	Map<String, ConceptClass> conceptClasses = new HashMap<>();

	Map<String, ConceptSource> conceptSources = new HashMap<>();

	// Store normalized name -> source mapping for fuzzy matching
	Map<String, List<ConceptSource>> normalizedConceptSources = new HashMap<>();

	Map<String, ConceptMapType> conceptMapTypes = new HashMap<>();

	Map<String, ConceptMap> conceptMaps = new HashMap<>();

	Map<String, Concept> concepts = new HashMap<>();

	// Cache for ConceptReferenceTerms keyed by "sourceId:code"
	Map<String, ConceptReferenceTerm> conceptReferenceTerms = new HashMap<>();

	// Cache for item URL lookups - persists across clearCache() calls since items are used for metadata only
	private final Map<String, Item> itemsByUrl = new HashMap<>();
	private final Set<String> checkedItemUrls = new HashSet<>();
	private boolean skipDbItemLookups = false;

	/**
	 * Pre-loads all concept sources into the cache to avoid repeated database queries.
	 */
	private void preloadConceptSources() {
		if (conceptSourcesPreloaded) {
			return;
		}
		for (ConceptSource source : conceptService.getAllConceptSources(true)) {
			// Cache by exact name
			conceptSources.put(source.getName(), source);
			// Also cache by normalized name for fuzzy matching
			String normalizedName = Utils.normalizeConceptSourceName(source.getName());
			normalizedConceptSources.computeIfAbsent(normalizedName, k -> new ArrayList<>()).add(source);
		}
		conceptSourcesPreloaded = true;
	}

	/**
	 * Pre-loads all concept datatypes into the cache.
	 */
	private void preloadConceptDatatypes() {
		if (conceptDatatypesPreloaded) {
			return;
		}
		for (ConceptDatatype datatype : conceptService.getAllConceptDatatypes()) {
			conceptDatatypes.put(datatype.getName(), datatype);
		}
		conceptDatatypesPreloaded = true;
	}

	/**
	 * Pre-loads all concept classes into the cache.
	 */
	private void preloadConceptClasses() {
		if (conceptClassesPreloaded) {
			return;
		}
		for (ConceptClass conceptClass : conceptService.getAllConceptClasses()) {
			conceptClasses.put(conceptClass.getName(), conceptClass);
		}
		conceptClassesPreloaded = true;
	}

	/**
	 * Pre-loads all concept map types into the cache.
	 */
	private void preloadConceptMapTypes() {
		if (conceptMapTypesPreloaded) {
			return;
		}
		for (ConceptMapType mapType : conceptService.getConceptMapTypes(true, true)) {
			conceptMapTypes.put(mapType.getName(), mapType);
		}
		conceptMapTypesPreloaded = true;
	}

	/**
	 * Gets the last successful item for a given URL. Results are cached to avoid
	 * repeated database queries for the same URL across batches and import phases.
	 * The cache persists across clearCache() calls since items are used for metadata only.
	 */
	public Item getLastSuccessfulItemByUrl(String url, ImportService importService) {
		if (url == null) {
			return null;
		}

		if (checkedItemUrls.contains(url)) {
			return itemsByUrl.get(url);
		}

		if (skipDbItemLookups) {
			return null;
		}

		Item item = importService.getLastSuccessfulItemByUrl(url, this);
		checkedItemUrls.add(url);
		if (item != null) {
			itemsByUrl.put(url, item);
		}
		return item;
	}

	/**
	 * Caches an item by its URL for fast lookup during the import.
	 * Called after successfully saving a concept or mapping to make it
	 * available for subsequent lookups (e.g., mapping phase looking up concept items)
	 * without a database query.
	 */
	public void cacheItem(Item item) {
		if (item != null && item.getUrl() != null && item.getState() != ItemState.ERROR) {
			itemsByUrl.put(item.getUrl(), item);
			checkedItemUrls.add(item.getUrl());
		}
	}

	/**
	 * When set to true, skips database lookups in getLastSuccessfulItemByUrl() for URLs
	 * not already in the cache. Used for first-time imports where no previous items exist,
	 * eliminating thousands of DB queries that would all return null.
	 */
	public void setSkipDbItemLookups(boolean skip) {
		this.skipDbItemLookups = skip;
	}

	public void clearCache() {
		conceptDatatypes.clear();
		conceptDatatypesPreloaded = false;
		conceptClasses.clear();
		conceptClassesPreloaded = false;
		conceptSources.clear();
		normalizedConceptSources.clear();
		conceptSourcesPreloaded = false;
		conceptMapTypes.clear();
		conceptMapTypesPreloaded = false;
		conceptMaps.clear();
		concepts.clear();
		conceptReferenceTerms.clear();

		// Note: itemsByUrl and checkedItemUrls are intentionally NOT cleared here.
		// They must persist across flush/clear cycles so the mapping phase can look up
		// concept items saved earlier without DB queries. For ~15,000 items this retains
		// ~15K Item objects + String keys in memory, which is acceptable. The entire
		// CacheService instance is GC'd when the import run completes.
	}

	public ConceptDatatype getConceptDatatypeByName(String name) {
		if (name == null || StringUtils.isBlank(name)) {
			return null;
		}

		// Ensure datatypes are preloaded
		preloadConceptDatatypes();

		ConceptDatatype conceptDatatype = conceptDatatypes.get(name);
		if (conceptDatatype == null) {
			// Fallback to database lookup for any missed datatypes
			conceptDatatype = conceptService.getConceptDatatypeByName(name);
			if (conceptDatatype != null) {
				conceptDatatypes.put(name, conceptDatatype);
			}
		}
		return conceptDatatype;
	}

	public ConceptClass getConceptClassByName(String name) {
		if (name == null || StringUtils.isBlank(name)) {
			return null;
		}

		// Ensure classes are preloaded
		preloadConceptClasses();

		ConceptClass conceptClass = conceptClasses.get(name);
		if (conceptClass == null) {
			// Fallback to database lookup for any missed classes
			conceptClass = conceptService.getConceptClassByName(name);
			if (conceptClass != null) {
				conceptClasses.put(name, conceptClass);
			}
		}
		return conceptClass;
	}

	/**
	 * If a concept name exists with an exact matching name (case-insensitive), then return it
	 * Else, do a normalized comparison, ignoring spaces, dashes, and underscores.  If exactly one match is found,
	 * then return it.  If more than one match is found, throw an Exception.
	 */
	public ConceptSource getConceptSourceByName(String name) {
		if (name == null || StringUtils.isBlank(name)) {
			return null;
		}

		// Ensure sources are preloaded
		preloadConceptSources();

		// Check exact match first
		ConceptSource conceptSource = conceptSources.get(name);
		if (conceptSource != null) {
			return conceptSource;
		}

		// Try case-insensitive exact match
		for (ConceptSource source : conceptSources.values()) {
			if (source.getName().equalsIgnoreCase(name)) {
				conceptSources.put(name, source);
				return source;
			}
		}

		// Try normalized/fuzzy match
		String normalizedName = Utils.normalizeConceptSourceName(name);
		List<ConceptSource> fuzzyMatches = normalizedConceptSources.get(normalizedName);
		if (fuzzyMatches != null && !fuzzyMatches.isEmpty()) {
			if (fuzzyMatches.size() > 1) {
				String msg = "There are " + fuzzyMatches.size() + " possible matching sources for " + name;
				throw new IllegalStateException(msg);
			}
			ConceptSource match = fuzzyMatches.get(0);
			conceptSources.put(name, match);
			return match;
		}

		return null;
	}

	/**
	 * Adds a newly created ConceptSource to the cache.
	 * Call this after creating and saving a new source to ensure subsequent lookups find it.
	 */
	public void addConceptSource(ConceptSource source) {
		conceptSources.put(source.getName(), source);
		String normalizedName = Utils.normalizeConceptSourceName(source.getName());
		normalizedConceptSources.computeIfAbsent(normalizedName, k -> new ArrayList<>()).add(source);
	}

	public ConceptMapType getConceptMapTypeByName(String name) {
		if (name == null || StringUtils.isBlank(name)) {
			return null;
		}

		// Ensure map types are preloaded
		preloadConceptMapTypes();

		ConceptMapType conceptMapType = conceptMapTypes.get(name);
		if (conceptMapType == null) {
			// Fallback to database lookup for any missed map types
			conceptMapType = conceptService.getConceptMapTypeByName(name);
			if (conceptMapType != null) {
				conceptMapTypes.put(name, conceptMapType);
			}
		}
		return conceptMapType;
	}

	public ConceptMap getConceptMapByUuid(String uuid, ImportService importService) {
		if (uuid == null || StringUtils.isBlank(uuid)) {
			return null;
		}

		ConceptMap conceptMap = conceptMaps.get(uuid);
		if (conceptMap != null) {
			return conceptMap;
		}

		conceptMap = importService.getConceptMapByUuid(uuid);
		if (conceptMap != null) {
			conceptMaps.put(uuid, conceptMap);
		}
		return conceptMap;
	}

	public Concept getConceptByUuid(String uuid) {
		if (uuid == null || StringUtils.isBlank(uuid)) {
			return null;
		}

		Concept concept = concepts.get(uuid);
		if (concept != null) {
			return concept;
		}

		concept = conceptService.getConceptByUuid(uuid);
		if (concept != null) {
			concepts.put(uuid, concept);
		}
		return concept;
	}
	
	public Concept getConceptWithSameAsMapping(String source, String code) {
		if (source == null || code == null) {
			return null;
		}
		String cacheKey = source + ":" + code;
		Concept concept = concepts.get(cacheKey);
		if (concept != null) {
			return concept;
		}

		concept = oclConceptService.getConceptWithSameAsMapping(code, source);
		if (concept != null) {
			concepts.put(cacheKey, concept);
		}

		return concept;
	}

	/**
	 * Gets a ConceptReferenceTerm by code and source, using the cache.
	 * The cache key is "sourceId:code" to uniquely identify terms.
	 */
	public ConceptReferenceTerm getConceptReferenceTermByCode(String code, ConceptSource source) {
		if (code == null || source == null || source.getId() == null) {
			return null;
		}

		String cacheKey = source.getId() + ":" + code;
		ConceptReferenceTerm term = conceptReferenceTerms.get(cacheKey);
		if (term == null) {
			term = conceptService.getConceptReferenceTermByCode(code, source);
			if (term != null) {
				conceptReferenceTerms.put(cacheKey, term);
			}
		}
		return term;
	}

	/**
	 * Adds a newly created ConceptReferenceTerm to the cache.
	 */
	public void addConceptReferenceTerm(ConceptReferenceTerm term) {
		if (term != null && term.getCode() != null && term.getConceptSource() != null
				&& term.getConceptSource().getId() != null) {
			String cacheKey = term.getConceptSource().getId() + ":" + term.getCode();
			conceptReferenceTerms.put(cacheKey, term);
		}
	}
}
