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
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheService {

	ConceptService conceptService;

	OclConceptService oclConceptService;

	// Track whether concept sources have been pre-loaded
	private boolean conceptSourcesPreloaded = false;

	public CacheService(ConceptService conceptService, OclConceptService oclConceptService) {
		this.conceptService = conceptService;
		this.oclConceptService = oclConceptService;
	}

	Map<String, ConceptDatatype> conceptDatatypes = new ConcurrentHashMap<String, ConceptDatatype>();

	Map<String, ConceptClass> conceptClasses = new ConcurrentHashMap<String, ConceptClass>();

	Map<String, ConceptSource> conceptSources = new ConcurrentHashMap<String, ConceptSource>();

	// Store normalized name -> source mapping for fuzzy matching
	Map<String, List<ConceptSource>> normalizedConceptSources = new ConcurrentHashMap<String, List<ConceptSource>>();

	Map<String, ConceptMapType> conceptMapTypes = new ConcurrentHashMap<String, ConceptMapType>();

	Map<String, ConceptMap> conceptMaps = new ConcurrentHashMap<String, ConceptMap>();

	Map<String, Concept> concepts = new ConcurrentHashMap<String, Concept>();

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

	public void clearCache() {
		conceptDatatypes.clear();
		conceptClasses.clear();
		conceptSources.clear();
		normalizedConceptSources.clear();
		conceptSourcesPreloaded = false;
		conceptMapTypes.clear();
		conceptMaps.clear();
		concepts.clear();
	}

	public ConceptDatatype getConceptDatatypeByName(String name) {
		if (name == null || StringUtils.isBlank(name)) {
			return null;
		}

		ConceptDatatype conceptDatatype = conceptDatatypes.get(name);
        if (conceptDatatype == null) {
            conceptDatatype = conceptService.getConceptDatatypeByName(name);
            if (conceptDatatype != null) {
                conceptDatatypes.put(name, conceptDatatype);
            }
        }
        return conceptDatatype;
    }

	public ConceptClass getConceptClassByName(String name) {
		ConceptClass conceptClass = conceptClasses.get(name);
		if (conceptClass != null) {
			return conceptClass;
		} else {
			conceptClass = conceptService.getConceptClassByName(name);
			if (conceptClass != null) {
				conceptClasses.put(name, conceptClass);
			}
			return conceptClass;
		}
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

		ConceptMapType conceptMapType = conceptMapTypes.get(name);
		if (conceptMapType != null) {
			return conceptMapType;
		} else {
			conceptMapType = conceptService.getConceptMapTypeByName(name);
			if (conceptMapType != null) {
				conceptMapTypes.put(name, conceptMapType);
			}
			return conceptMapType;
		}
    }

	public ConceptMap getConceptMapByUuid(String uuid, ImportService importService) {
		if (uuid == null || StringUtils.isBlank(uuid)) {
			return null;
		}

		ConceptMap conceptMap = conceptMaps.get(uuid);
		if (conceptMap != null) {
			return conceptMap;
		} else {
			conceptMap = importService.getConceptMapByUuid(uuid);
			if (conceptMap != null) {
				conceptMaps.put(uuid, conceptMap);
			}
			return conceptMap;
		}
	}

	public Concept getConceptByUuid(String uuid) {
		if (uuid == null || StringUtils.isBlank(uuid)) {
			return null;
		}

		Concept concept = concepts.get(uuid);
		if (concept != null) {
			return concept;
		} else {
			concept = conceptService.getConceptByUuid(uuid);
			if (concept != null) {
				concepts.put(uuid, concept);
			}
			return concept;
		}
    }
	
	public Concept getConceptWithSameAsMapping(String source, String code) {
		String cacheKey = source + ":" + code;
		Concept concept = concepts.get(cacheKey);
		if (concept == null) {
			concept = oclConceptService.getConceptWithSameAsMapping(code, source);
			if (concept != null) {
				concepts.put(cacheKey, concept);
			}
		}
		
		return concept;
	}
}
