/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.importer;

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
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.ValidationType;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclConcept.Description;
import org.openmrs.module.openconceptlab.client.OclConcept.Extras;
import org.openmrs.module.openconceptlab.client.OclMapping;
import org.openmrs.module.openconceptlab.client.OclMapping.MapType;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class Saver {

	protected final Log log = LogFactory.getLog(getClass());

	private static final Object CREATE_CONCEPT_REFERENCE_TERM_LOCK = new Object();

	private static final Object CREATE_CONCEPT_CLASS_LOCK = new Object();

	private static final Object CREATE_CONCEPT_SOURCE_LOCK = new Object();

	private ConceptService conceptService;

	private ImportService importService;

    public void setConceptService(ConceptService conceptService) {
	    this.conceptService = conceptService;
    }


    public void setImportService(ImportService importService) {
	    this.importService = importService;
    }

	/**
	 * @param oclConcept
	 * @param importQueue
	 * @throws ImportException
	 * @should save new concept
	 * @should add new names to concept
	 * @should anImport name type in concept
	 * @should anImport names with different uuids
	 * @should void names from concept
	 * @should add new descriptions to concept
	 * @should void descriptions from concept
	 * @should retire concept
	 * @should unretire concept
	 * @should fail if datatype missing
	 * @should create concept class if missing
	 * @should change duplicate synonym to index term
	 * @should change duplicate fully specified name to index term
	 * @should skip updating concept if it is already up to date
	 */
	public Item saveConcept(CacheService cacheService, Import anImport, OclConcept oclConcept) throws ImportException {
		Item item = importService.getLastSuccessfulItemByUrl(oclConcept.getUrl());
		if(item != null && item.getVersionUrl().equals(oclConcept.getVersionUrl())){
			return new Item(anImport, oclConcept, ItemState.UP_TO_DATE);
		}
		
		Concept concept = toConcept(cacheService, oclConcept);

		boolean added = false;
		if (concept.getId() == null) {
			added = true;
		}

		boolean retryOnFailure = true;
		boolean retryOnDuplicateNames = true;
		List<String> resolutionLog = new ArrayList<String>();
		while (true) {
			try {
				try {
					ValidationType validationType = importService.getSubscription().getValidationType();
					if (ValidationType.FULL.equals(validationType)) {
						conceptService.saveConcept(concept);
					} else if(ValidationType.NONE.equals(validationType)){
						importService.updateConceptWithoutValidation(concept);
					}

					return new Item(anImport, oclConcept, added ? ItemState.ADDED : ItemState.UPDATED);
				}
				catch (DuplicateConceptNameException e) {
					if (!retryOnDuplicateNames) {
						throw new ImportException("Cannot import concept " + concept.getUuid() + ", tried:\n" + logToString(resolutionLog), e);
					}

					Context.clearSession();
					cacheService.clearCache();
					anImport = importService.getImport(anImport.getImportId());
					concept = toConcept(cacheService, oclConcept);

					resolutionLog.add("Fixing duplicate names for concept " + concept.getUuid() + " after failure due to " + e.getMessage());
					changeDuplicateNamesToIndexTerms(concept, resolutionLog);
					resolutionLog.add("Done fixing duplicate names for concept " + concept.getUuid());

					retryOnDuplicateNames = false;
				}
			} catch (ImportException e) {
				throw e; //re-throw, failed to resolved duplicates
			} catch (Exception e) {
				if (!retryOnFailure) {
					throw new ImportException("Cannot import concept " + concept.getUuid() + ", tried:\n" + logToString(resolutionLog), e);
				}

				Context.clearSession();
				cacheService.clearCache();
				anImport = importService.getImport(anImport.getImportId());
				concept = toConcept(cacheService, oclConcept);

				resolutionLog.add("Retrying import of concept " + concept.getUuid() + " after failure due to '" + e.getMessage() + "'");

				retryOnFailure = false;
			}
		}
	}

	public String logToString(List<String> log) {
		return StringUtils.join(log, "\n");
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
			synchronized (CREATE_CONCEPT_CLASS_LOCK) {
				conceptClass = cacheService.getConceptClassByName(oclConcept.getConceptClass());
				if (conceptClass == null) {
					conceptClass = new ConceptClass();
					conceptClass.setName(oclConcept.getConceptClass());
					conceptClass.setDescription("Imported from Open Concept Lab");
					conceptService.saveConceptClass(conceptClass);
				}
			}
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

			numeric.setAllowDecimal(extras.getPrecise());
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

	private void changeDuplicateNamesToIndexTerms(Concept concept, List<String> resolutionLog) {
		List<ConceptName> conceptNames = importService.changeDuplicateConceptNamesToIndexTerms(concept);
		for (ConceptName conceptName : conceptNames) {
			resolutionLog.add("Changed name '" + conceptName.getName() + "' in locale " + conceptName.getLocale() + " to index term");
		}

		//Make sure there is at least one fully specified name
		boolean hasFullySpecifiedName = false;
		for (ConceptName name : concept.getNames()) {
			if (ConceptNameType.FULLY_SPECIFIED.equals(name.getConceptNameType())) {
				hasFullySpecifiedName = true;
				break;
			}
		}

		if (!hasFullySpecifiedName) {
			for (ConceptName name : concept.getNames()) {
				if (!ConceptNameType.INDEX_TERM.equals(name.getConceptNameType())) {
					name.setConceptNameType(ConceptNameType.FULLY_SPECIFIED);
					break;
				}
			}
		}
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
	 * @should anImport mapping only if it has been updated since last import
	 */
	public Item saveMapping(CacheService cacheService, Import update, OclMapping oclMapping) {
		Item oldMappingItem = importService.getLastSuccessfulItemByUrl(oclMapping.getUrl());
		if(oldMappingItem!= null && isMappingUpToDate(oldMappingItem, oclMapping)){
			return new Item(update, oclMapping, ItemState.UP_TO_DATE);
		}

		final Item item;

		Item fromItem = null;
		Concept fromConcept = null;
		if (!StringUtils.isBlank(oclMapping.getFromConceptUrl())) {
			fromItem = importService.getLastSuccessfulItemByUrl(oclMapping.getFromConceptUrl());
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
			toItem = importService.getLastSuccessfulItemByUrl(oclMapping.getToConceptUrl());
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

			importService.updateConceptWithoutValidation(fromConcept);
		} else {
			ConceptSource toSource = cacheService.getConceptSourceByName(oclMapping.getToSourceName());
			if (toSource == null) {
				synchronized (CREATE_CONCEPT_SOURCE_LOCK) {
					toSource = cacheService.getConceptSourceByName(oclMapping.getToSourceName());
					if (toSource == null) {
						toSource = new ConceptSource();
						toSource.setName(oclMapping.getToSourceName());
						toSource.setDescription("Imported from " + oclMapping.getUrl());
						conceptService.saveConceptSource(toSource);
					}
				}
			}

			String mapTypeName = oclMapping.getMapType().replace("-", "_");
			ConceptMapType mapType = cacheService.getConceptMapTypeByName(mapTypeName);
			if (mapType == null) {
				return new Item(update, oclMapping, ItemState.ERROR, "Map type " + mapTypeName + " does not exist");
			}

			if (fromConcept != null) {
				ConceptMap conceptMap = importService.getConceptMapByUuid(oclMapping.getExternalId());

				ConceptReferenceTerm term = createOrUpdateConceptReferenceTerm(oclMapping, conceptMap, toSource);

				if (conceptMap != null) {
					if (!conceptMap.getConcept().equals(fromConcept)) {
						//Concept changed, it would be unusual, but still probable
						Concept previousConcept = conceptMap.getConcept();

						previousConcept.removeConceptMapping(conceptMap);
						importService.updateConceptWithoutValidation(previousConcept);

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

				importService.updateConceptWithoutValidation(fromConcept);
			} else {
				return new Item(update, oclMapping, ItemState.ERROR, "Mapping " + oclMapping.getUrl() + " is not supported");
			}
		}
		return item;
	}
	
	/**
	 * @param oldItem
	 * @param newMapping
	 * @return boolean
	 * @should should return true if any of updatedOn is null
	 * @should should return false if both updatedOn are null
	 * @should should return if mapping's updatedOn is after
	 */
	
	public boolean isMappingUpToDate(Item oldItem, OclMapping newMapping) {
		Date oldUpdatedOn = oldItem.getUpdatedOn();
		Date newUpdatedOn = newMapping.getUpdatedOn();
		//mapping never was updated
		if(oldUpdatedOn==null&&newUpdatedOn==null){
			return true;
		}
		//mapping was updated at least once
		else if(oldUpdatedOn!=null&&newUpdatedOn!=null){
				return newUpdatedOn.equals(oldUpdatedOn);
		}
		//this is first anImport - old version updatedOn is null
		else return false;
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
			synchronized (CREATE_CONCEPT_REFERENCE_TERM_LOCK) {
	            term = conceptService.getConceptReferenceTermByCode(oclMapping.getToConceptCode(), toSource);

	            if (term == null) {
	            	term = new ConceptReferenceTerm();
	            	term.setConceptSource(toSource);
	        		term.setCode(oclMapping.getToConceptCode());

	        		conceptService.saveConceptReferenceTerm(term);
	            }
            }
		}

		if (term.getRetired() != oclMapping.isRetired()) {
			term.setRetired(oclMapping.isRetired());
			if (oclMapping.isRetired()) {
				term.setRetireReason("OCL subscription");
			} else {
				term.setRetireReason(null);
				term.setRetiredBy(null);
			}

			importService.updateConceptReferenceTermWithoutValidation(term);
		}

		return term;
	}

	Item updateOrAddSetMemebersFromOcl(Import update, OclMapping oclMapping, Concept set, Concept member) {
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

	private Item updateOrAddAnswersFromOcl(Import update, OclMapping oclMapping, Concept question, Concept answer) {
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

	private void updateOrAddNamesFromOcl(Concept concept, OclConcept oclConcept) {
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
			if (!nameFound && !name.getVoided()) {
				name.setVoided(true);
				name.setVoidReason("Removed from OCL");
			}
		}
	}

	void addDescriptionsFromOcl(Concept concept, OclConcept oclConcept) {
		for (OclConcept.Description oclDescription : oclConcept.getDescriptions()) {
			if (StringUtils.isBlank(oclDescription.getDescription())) {
				continue;
			}

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
			for (Description oclDescription : oclConcept.getDescriptions()) {
				if (isMatch(oclDescription, description)) {
					if (!StringUtils.isBlank(oclDescription.getDescription())) {
						//Blank descriptions are invalid and will be removed
						descriptionFound = true;
					}
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
