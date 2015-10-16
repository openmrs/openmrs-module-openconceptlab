/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.updater;

import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSet;
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.DuplicateConceptNameException;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.CacheService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclConcept.Description;
import org.openmrs.module.openconceptlab.client.OclConcept.Extras;
import org.openmrs.module.openconceptlab.client.OclConcept.Name;
import org.openmrs.module.openconceptlab.client.OclMapping;
import org.openmrs.module.openconceptlab.client.OclMapping.MapType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("openconceptlab.importer")
public class Importer {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	@Autowired
	@Qualifier("conceptService")
	ConceptService conceptService;
	
	@Autowired
	UpdateService updateService;
	
	/**
	 * @param oclConcept
	 * @param importQueue
	 * @throws ImportException
	 * @should save new concept
	 * @should add new names to concept
	 * @should update name type in concept
	 * @should update names with different uuids
	 * @should void names from concept
	 * @should add new descriptions to concept
	 * @should void descriptions from concept
	 * @should retire concept
	 * @should unretire concept
	 * @should fail if concept class missing
	 * @should fail if datatype missing
	 * @should change duplicate synonym to index term
	 */
	public Item importConcept(CacheService cacheService, Update update, OclConcept oclConcept) throws ImportException {
		Concept concept = toConcept(cacheService, oclConcept);
		
		final Item item;
		if (concept.getId() == null) {
			item = new Item(update, oclConcept, ItemState.ADDED);
		} else {
			item = new Item(update, oclConcept, ItemState.UPDATED);
		}
		
		boolean trySaving = true;
		while (trySaving) {
			try {
				conceptService.saveConcept(concept);
				trySaving = false;
			}
			catch (DuplicateConceptNameException e) {
				Context.clearSession();
				cacheService.clearCache();
				log.info("Attempting to fix " + e.getMessage() + " for concept with UUID " + concept.getUuid());
				trySaving = fixSynonymToIndexTerm(oclConcept, e);
				concept = toConcept(cacheService, oclConcept);
				if (!trySaving) {
					throw new ImportException("Cannot save concept with UUID " + concept.getUuid() + " after attempting to fix duplicates", e);
				}
			}
			catch (Exception e) {
				throw new ImportException("Cannot save concept with UUID " + concept.getUuid(), e);
			}
		}
		
		return item;
	}
	
	public Concept toConcept(CacheService cacheService, OclConcept oclConcept) throws ImportException {
		ConceptDatatype datatype = cacheService.getConceptDatatypeByName(oclConcept.getDatatype());
		if (datatype == null) {
			throw new ImportException("Datatype '" + oclConcept.getDatatype() + "' is not supported by OpenMRS");
		}
		
		Concept concept = cacheService.getConceptByUuid(oclConcept.getExternalId());
		if (concept == null) {
			if (datatype.getUuid().equals(ConceptDatatype.NUMERIC_UUID)) {
				concept = new ConceptNumeric();
			} else {
				concept = new Concept();
			}
			concept.setUuid(oclConcept.getExternalId());
		}
		ConceptClass conceptClass = cacheService.getConceptClassByName(oclConcept.getConceptClass());
		if (conceptClass == null) {
			throw new ImportException("Concept class '" + oclConcept.getConceptClass() + "' is missing");
		}
		concept.setConceptClass(conceptClass);
		
		concept.setDatatype(datatype);
		
		if (concept instanceof ConceptNumeric) {
			ConceptNumeric numeric = (ConceptNumeric) concept;
			
			Extras extras = oclConcept.getExtras();
			
			numeric.setHiAbsolute(extras.getHiAbsolute());
			
			numeric.setHiCritical(extras.getHiCritical());
			
			numeric.setHiNormal(extras.getHiNormal());
			
			numeric.setLowAbsolute(extras.getLowAbsolute());
			
			numeric.setLowCritical(extras.getLowCritical());
			
			numeric.setLowNormal(extras.getLowNormal());
			
			numeric.setUnits(extras.getUnits());
			
			numeric.setPrecise(extras.getPrecise());
		}
		
		concept.setRetired(oclConcept.isRetired());
		if (oclConcept.isRetired()) {
			concept.setRetireReason("Retired in OCL");
		} else {
			concept.setRetireReason(null);
			concept.setRetiredBy(null);
		}
		
		voidNamesRemovedFromOcl(concept, oclConcept);
		
		updateOrAddNamesFromOcl(concept, oclConcept);
		
		removeDescriptionsRemovedFromOcl(concept, oclConcept);
		
		addDescriptionsFromOcl(concept, oclConcept);
		
		return concept;
	}

	private boolean fixSynonymToIndexTerm(OclConcept concept, DuplicateConceptNameException e) {
	    Pattern pattern = Pattern.compile("^'([^']*)' is a duplicate name in locale '([^']*)'$");
	    String message = e.getMessage();
	    Matcher matcher = pattern.matcher(message);
	    if (matcher.find()) {
	    	String name = matcher.group(1);
	    	Locale locale = LocaleUtils.toLocale(matcher.group(2));
	    	for (Name conceptName : concept.getNames()) {
	    		if (conceptName.getName().equals(name) && conceptName.getLocale().equals(locale)) {
	    			if (StringUtils.isBlank(conceptName.getNameType())) {
	    				conceptName.setNameType(ConceptNameType.INDEX_TERM.toString());
	    				conceptName.setLocalePreferred(false);
	    				return true;
	    			}
	    		}
	    	}
	    }
	    return false;
    }
	
	/**
	 * @param update
	 * @param oclMapping
	 * @return
	 * @should add concept answer
	 * @should add concept set member
	 * @should remove concept answer
	 * @should remove concept set member
	 * @should add concept mapping and term
	 * @should add concept mapping and unretire term
	 * @should remove concept mapping and retire term
	 */
	@Transactional
	public Item importMapping(CacheService cacheService, Update update, OclMapping oclMapping) {
		final Item item;
		
		Item fromItem = null;
		Concept fromConcept = null;
		if (!StringUtils.isBlank(oclMapping.getFromConceptUrl())) {
			fromItem = updateService.getLastSuccessfulItemByUrl(oclMapping.getFromConceptUrl());
			if (fromItem != null) {
				fromConcept = cacheService.getConceptByUuid(fromItem.getUuid());
			}
			
			if (fromConcept == null) {
				return new Item(update, oclMapping, ItemState.ERROR, "Cannot create mapping from concept with URL "
				        + oclMapping.getFromConceptUrl() + ", because the concept has not been imported");
			}
		}
		
		Item toItem = null;
		Concept toConcept = null;
		if (!StringUtils.isBlank(oclMapping.getToConceptUrl())) {
			toItem = updateService.getLastSuccessfulItemByUrl(oclMapping.getToConceptUrl());
			if (toItem != null) {
				toConcept = cacheService.getConceptByUuid(toItem.getUuid());
			}
			
			if (toConcept == null) {
				return new Item(update, oclMapping, ItemState.ERROR, "Cannot create mapping to concept with URL "
				        + oclMapping.getToConceptUrl() + ", because the concept has not been imported");
			}
		}
		
		if (MapType.Q_AND_A.equals(oclMapping.getMapType()) || MapType.SET.equals(oclMapping.getMapType())) {
			if (oclMapping.getMapType().equals(MapType.Q_AND_A)) {
				item = updateOrAddAnswersFromOcl(update, oclMapping, fromConcept, toConcept);
			} else {
				item = updateOrAddSetMemebersFromOcl(update, oclMapping, fromConcept, toConcept);
			}
			
			conceptService.saveConcept(fromConcept);
		} else {
			ConceptSource toSource = cacheService.getConceptSourceByName(oclMapping.getToSourceName());
			if (toSource == null) {
				toSource = new ConceptSource();
				toSource.setName(oclMapping.getToSourceName());
				toSource.setDescription("Imported from " + oclMapping.getUrl());
				conceptService.saveConceptSource(toSource);
			}
			
			String mapTypeName = oclMapping.getMapType().replace("-", "_");
			ConceptMapType mapType = cacheService.getConceptMapTypeByName(mapTypeName);
			if (mapType == null) {
				return new Item(update, oclMapping, ItemState.ERROR, "Map type " + mapTypeName + " does not exist");
			}
			
			if (fromConcept != null) {
				ConceptMap conceptMap = updateService.getConceptMapByUuid(oclMapping.getExternalId());
				
				ConceptReferenceTerm term = createOrUpdateConceptReferenceTerm(oclMapping, conceptMap, toSource);
				
				if (conceptMap != null) {
					if (!conceptMap.getConcept().equals(fromConcept)) {
						//Concept changed, it would be unusual, but still probable
						Concept previousConcept = conceptMap.getConcept();
						previousConcept.removeConceptMapping(conceptMap);
						conceptService.saveConcept(previousConcept);
						
						fromConcept.addConceptMapping(conceptMap);
					}
					
					if (Boolean.TRUE.equals(oclMapping.getRetired())) {
						item = new Item(update, oclMapping, ItemState.RETIRED);
						fromConcept.removeConceptMapping(conceptMap);
					} else {
						item = new Item(update, oclMapping, ItemState.UPDATED);
						conceptMap.setConceptMapType(mapType);
					}
				} else {
					conceptMap = new ConceptMap();
					conceptMap.setUuid(oclMapping.getExternalId());
					conceptMap.setConceptReferenceTerm(term);
					conceptMap.setConceptMapType(mapType);
					fromConcept.addConceptMapping(conceptMap);
					
					item = new Item(update, oclMapping, ItemState.ADDED);
				}
				
				conceptService.saveConcept(fromConcept);
			} else {
				return new Item(update, oclMapping, ItemState.ERROR, "Mapping " + oclMapping.getUrl() + " is not supported");
			}
		}
		return item;
	}
	
	ConceptReferenceTerm createOrUpdateConceptReferenceTerm(OclMapping oclMapping, ConceptMap conceptMap,
	        ConceptSource toSource) {
		ConceptReferenceTerm term = null;
		if (conceptMap == null) {
			term = conceptService.getConceptReferenceTermByCode(oclMapping.getToConceptCode(), toSource);
		} else {
			term = conceptMap.getConceptReferenceTerm();
		}
		
		if (term == null) {
			term = new ConceptReferenceTerm();
		}
		
		term.setConceptSource(toSource);
		term.setCode(oclMapping.getToConceptCode());
		
		if (term.isRetired() != oclMapping.isRetired()) {
			term.setRetired(oclMapping.isRetired());
			if (oclMapping.isRetired()) {
				term.setRetireReason("OCL subscription");
			} else {
				term.setRetireReason(null);
				term.setRetiredBy(null);
			}
		}
		
		conceptService.saveConceptReferenceTerm(term);
		return term;
	}
	
	Item updateOrAddSetMemebersFromOcl(Update update, OclMapping oclMapping, Concept set, Concept member) {
		Item item = null;
		
		boolean found = false;
		Iterator<ConceptSet> it = set.getConceptSets().iterator();
		while (it.hasNext()) {
			ConceptSet conceptSet = it.next();
			
			if (conceptSet.getUuid().equals(oclMapping.getExternalId())) {
				found = true;
				
				if (Boolean.TRUE.equals(oclMapping.getRetired())) {
					item = new Item(update, oclMapping, ItemState.RETIRED);
					it.remove();
				} else {
					item = new Item(update, oclMapping, ItemState.UPDATED);
					conceptSet.setConcept(member);
				}
				
				break;
			}
		}
		
		if (!found) {
			ConceptSet conceptSet = new ConceptSet();
			conceptSet.setConceptSet(set);
			conceptSet.setConcept(member);
			conceptSet.setUuid(oclMapping.getExternalId());
			conceptSet.setSortWeight(1.0);
			set.getConceptSets().add(conceptSet);
			item = new Item(update, oclMapping, ItemState.ADDED);
		}
		
		return item;
	}
	
	Item updateOrAddAnswersFromOcl(Update update, OclMapping oclMapping, Concept question, Concept answer) {
		Item item = null;
		
		boolean found = false;
		Iterator<ConceptAnswer> it = question.getAnswers().iterator();
		while (it.hasNext()) {
			ConceptAnswer conceptAnswer = it.next();
			
			if (conceptAnswer.getUuid().equals(oclMapping.getExternalId())) {
				found = true;
				
				if (Boolean.TRUE.equals(oclMapping.getRetired())) {
					item = new Item(update, oclMapping, ItemState.RETIRED);
					it.remove();
				} else {
					item = new Item(update, oclMapping, ItemState.UPDATED);
					conceptAnswer.setAnswerConcept(answer);
				}
				
				break;
			}
		}
		
		if (!found) {
			ConceptAnswer conceptAnswer = new ConceptAnswer(answer);
			conceptAnswer.setUuid(oclMapping.getExternalId());
			question.addAnswer(conceptAnswer);
			item = new Item(update, oclMapping, ItemState.ADDED);
		}
		
		return item;
	}
	
	void updateOrAddNamesFromOcl(Concept concept, OclConcept oclConcept) {
		for (OclConcept.Name oclName : oclConcept.getNames()) {
			ConceptNameType oclNameType = oclName.getNameType() != null ? ConceptNameType.valueOf(oclName.getNameType())
			        : null;
			
			boolean nameFound = false;
			for (ConceptName name : concept.getNames(true)) {
				if (isMatch(oclName, name)) {
					//Let's make sure all is the same
					name.setName(oclName.getName());
					name.setLocale(oclName.getLocale());
					name.setConceptNameType(oclNameType);
					name.setLocalePreferred(oclName.isLocalePreferred());
					
					//Unvoiding if necessary
					name.setVoided(false);
					name.setVoidReason(null);
					name.setVoidedBy(null);
					
					nameFound = true;
					break;
				}
			}
			
			if (!nameFound) {
				ConceptName name = new ConceptName(oclName.getName(), oclName.getLocale());
				name.setUuid(oclName.getExternalId());
				name.setConceptNameType(oclNameType);
				name.setLocalePreferred(oclName.isLocalePreferred());
				
				concept.addName(name);
			}
		}
	}
	
	void voidNamesRemovedFromOcl(Concept concept, OclConcept oclConcept) {
		for (ConceptName name : concept.getNames(true)) {
			boolean nameFound = false;
			for (OclConcept.Name oclName : oclConcept.getNames()) {
				if (isMatch(oclName, name)) {
					nameFound = true;
					break;
				}
			}
			if (!nameFound && !name.isVoided()) {
				name.setVoided(true);
				name.setVoidReason("Removed from OCL");
			}
		}
	}
	
	void addDescriptionsFromOcl(Concept concept, OclConcept oclConcept) {
		for (OclConcept.Description oclDescription : oclConcept.getDescriptons()) {
			boolean nameFound = false;
			for (ConceptDescription description : concept.getDescriptions()) {
				if (isMatch(oclDescription, description)) {
					//Let's make sure all is the same
					description.setDescription(oclDescription.getDescription());
					description.setLocale(oclDescription.getLocale());
					nameFound = true;
					break;
				}
			}
			
			if (!nameFound) {
				ConceptDescription description = new ConceptDescription(oclDescription.getDescription(),
				        oclDescription.getLocale());
				description.setUuid(oclDescription.getExternalId());
				concept.addDescription(description);
			}
		}
	}
	
	void removeDescriptionsRemovedFromOcl(Concept concept, OclConcept oclConcept) {
		for (Iterator<ConceptDescription> it = concept.getDescriptions().iterator(); it.hasNext();) {
			ConceptDescription description = it.next();
			boolean descriptionFound = false;
			for (Description oclDescription : oclConcept.getDescriptons()) {
				if (isMatch(oclDescription, description)) {
					descriptionFound = true;
					break;
				}
			}
			if (!descriptionFound) {
				it.remove();
			}
		}
	}
	
	public boolean isMatch(OclConcept.Name oclName, ConceptName name) {
		return new EqualsBuilder().append(name.getUuid(), oclName.getExternalId()).isEquals();
	}
	
	public boolean isMatch(OclConcept.Description oclDescription, ConceptDescription description) {
		return new EqualsBuilder().append(description.getUuid(), oclDescription.getExternalId()).isEquals();
	}
}
