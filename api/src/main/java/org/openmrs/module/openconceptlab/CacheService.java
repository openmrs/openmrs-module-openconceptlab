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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptService;
import org.springframework.cache.annotation.Cacheable;

public class CacheService {

	ConceptService conceptService;

	public CacheService(ConceptService conceptService) {
		this.conceptService = conceptService;
	}

	Map<String, ConceptDatatype> conceptDatatypes = new ConcurrentHashMap<String, ConceptDatatype>();

	Map<String, ConceptClass> conceptClasses = new ConcurrentHashMap<String, ConceptClass>();

	Map<String, ConceptSource> conceptSources = new ConcurrentHashMap<String, ConceptSource>();

	Map<String, ConceptMapType> conceptMapTypes = new ConcurrentHashMap<String, ConceptMapType>();

	Map<String, Concept> conceptsByUuids = new ConcurrentHashMap<String, Concept>();

	public void clearCache() {
		conceptDatatypes.clear();
		conceptClasses.clear();
		conceptSources.clear();
		conceptMapTypes.clear();
		conceptsByUuids.clear();
	}

	@Cacheable(value = "conceptDataType")
	public ConceptDatatype getConceptDatatypeByName(String name) {
		ConceptDatatype conceptDatatype = conceptService.getConceptDatatypeByName(name);
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

	public ConceptSource getConceptSourceByName(String name) {
		ConceptSource conceptSource = conceptSources.get(name);
		if (conceptSource != null) {
			return conceptSource;
		} else {
			conceptSource = conceptService.getConceptSourceByName(name);
			if (conceptSource != null) {
				conceptSources.put(name, conceptSource);
			}
			return conceptSource;
		}
	}

	public ConceptMapType getConceptMapTypeByName(String name) {
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

	public Concept getConceptByUuid(String uuid) {
		Concept concept = conceptsByUuids.get(uuid);
		if (concept != null) {
			return concept;
		} else {
			concept = conceptService.getConceptByUuid(uuid);
			if (concept != null) {
				conceptsByUuids.put(uuid, concept);
			}
			return concept;
		}
    }
}
