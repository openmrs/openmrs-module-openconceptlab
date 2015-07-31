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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("openconceptlab.cacheService")
public class CacheService {
	
	@Autowired
	ConceptService conceptService;
	
	Map<String, Integer> conceptDatatypes = new ConcurrentHashMap<String, Integer>();
	
	Map<String, Integer> conceptClasses = new ConcurrentHashMap<String, Integer>();
	
	Map<String, Integer> conceptSources = new ConcurrentHashMap<String, Integer>();
	
	Map<String, Integer> conceptMapTypes = new ConcurrentHashMap<String, Integer>();
	
	Map<String, Integer> conceptsByUuids = new ConcurrentHashMap<String, Integer>();
	
	public void clearCache() {
		conceptDatatypes.clear();
		conceptClasses.clear();
		conceptSources.clear();
		conceptMapTypes.clear();
		conceptsByUuids.clear();
	}
	
	public ConceptDatatype getConceptDatatypeByName(String name) {
		Integer id = conceptDatatypes.get(name);
		if (id != null) {
			ConceptDatatype conceptDatatype = conceptService.getConceptDatatype(id);
			return conceptDatatype;
		} else {
			ConceptDatatype conceptDatatype = conceptService.getConceptDatatypeByName(name);
			if (conceptDatatype != null) {
				conceptDatatypes.put(name, conceptDatatype.getId());
			}
			return conceptDatatype;
		}
	}
	
	public ConceptClass getConceptClassByName(String name) {
		Integer id = conceptClasses.get(name);
		if (id != null) {
			ConceptClass conceptClass = conceptService.getConceptClass(id);
			return conceptClass;
		} else {
			ConceptClass conceptClass = conceptService.getConceptClassByName(name);
			if (conceptClass != null) {
				conceptClasses.put(name, conceptClass.getId());
			}
			return conceptClass;
		}
	}
	
	public ConceptSource getConceptSourceByName(String name) {
		Integer id = conceptSources.get(name);
		if (id != null) {
			ConceptSource conceptSource = conceptService.getConceptSource(id);
			return conceptSource;
		} else {
			ConceptSource conceptSource = conceptService.getConceptSourceByName(name);
			if (conceptSource != null) {
				conceptSources.put(name, conceptSource.getId());
			}
			return conceptSource;
		}
	}

	public ConceptMapType getConceptMapTypeByName(String name) {
		Integer id = conceptMapTypes.get(name);
		if (id != null) {
			ConceptMapType conceptMapType = conceptService.getConceptMapType(id);
			return conceptMapType;
		} else {
			ConceptMapType conceptMapType = conceptService.getConceptMapTypeByName(name);
			if (conceptMapType != null) {
				conceptMapTypes.put(name, conceptMapType.getId());
			}
			return conceptMapType;
		}
    }

	public Concept getConceptByUuid(String uuid) {
		Integer id = conceptsByUuids.get(uuid);
		if (id != null) {
			Concept concept = conceptService.getConcept(id);
			return concept;
		} else {
			Concept concept = conceptService.getConceptByUuid(uuid);
			if (concept != null) {
				conceptsByUuids.put(uuid, concept.getId());
			}
			return concept;
		}
    }
}
