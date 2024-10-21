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

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptComplex;
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
import org.openmrs.module.openconceptlab.Utils;
import org.openmrs.module.openconceptlab.ValidationType;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclConcept.Description;
import org.openmrs.module.openconceptlab.client.OclConcept.Extras;
import org.openmrs.module.openconceptlab.client.OclMapping;
import org.openmrs.module.openconceptlab.client.OclMapping.MapType;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static org.openmrs.module.openconceptlab.Utils.version5Uuid;

public class Saver {

	private static final Logger log = LoggerFactory.getLogger(Saver.class);

	private static final Object CREATE_CONCEPT_REFERENCE_TERM_LOCK = new Object();

	private static final Object CREATE_CONCEPT_CLASS_LOCK = new Object();

	private static final Object CREATE_CONCEPT_SOURCE_LOCK = new Object();

	private static final Object CREATE_MAP_TYPE_LOCK = new Object();

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
	 * @throws ImportException
	 */
	public Item saveConcept(final CacheService cacheService, final Import anImport, final OclConcept oclConcept) throws ImportException {
		Import thisImport = anImport;

		Item item = importService.getLastSuccessfulItemByUrl(oclConcept.getUrl(), cacheService);
		if (item != null && item.getVersionUrl().equals(oclConcept.getVersionUrl())) {
			return new Item(thisImport, oclConcept, ItemState.UP_TO_DATE);
		}

		Concept concept;
		try {
			concept = toConcept(cacheService, oclConcept);
		} catch (Exception e) {
			throw new ImportException("Cannot create concept " + oclConcept.getVersionUrl(), e);
		}

		boolean added = concept.getId() == null;

		boolean retryOnFailure = true;
		boolean retryOnDuplicateNames = true;
		List<String> resolutionLog = new ArrayList<String>();
		while (true) {
			try {
				try {
					ValidationType validationType;
					if (importService.getSubscription() != null) {
						validationType = importService.getSubscription().getValidationType();
					} else {
						validationType = ValidationType.FULL;
					}

					if (ValidationType.FULL.equals(validationType)) {
						conceptService.saveConcept(concept);
					} else if (ValidationType.NONE.equals(validationType)) {
						importService.updateConceptWithoutValidation(concept);
					}

					return new Item(thisImport, oclConcept, added ? ItemState.ADDED : ItemState.UPDATED);
				} catch (DuplicateConceptNameException e) {
					if (!retryOnDuplicateNames) {
						throw new ImportException("Cannot import concept " + concept.getUuid() + ", tried:\n" + logToString(resolutionLog), e);
					}

					Context.clearSession();
					cacheService.clearCache();
					thisImport = importService.getImport(thisImport.getImportId());
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
				thisImport = importService.getImport(thisImport.getImportId());
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
		convertConceptDatatypes(oclConcept);

		Extras extras = oclConcept.getExtras();
		if (extras == null) {
			extras = new Extras();
		}

		ConceptDatatype datatype = cacheService.getConceptDatatypeByName(oclConcept.getDatatype());
		if (datatype == null) {
			throw new ImportException("Datatype '" + oclConcept.getDatatype() + "' is not supported by OpenMRS");
		}

		Concept concept = null;
		if (oclConcept.getExternalId() != null) {
			concept = cacheService.getConceptByUuid(oclConcept.getExternalId());
		}

		// If a match is not made on UUID, attempt to make a match on SAME-AS mapping
		if (concept == null) {
			concept = cacheService.getConceptWithSameAsMapping(oclConcept.getSource(), oclConcept.getId());
		}
		
		if (concept == null) {
			if (datatype.getUuid().equals(ConceptDatatype.NUMERIC_UUID)) {
				concept = new ConceptNumeric();
			} else if (datatype.getUuid().equals(ConceptDatatype.COMPLEX_UUID)) {
				concept = new ConceptComplex();
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

		if (concept instanceof ConceptComplex) {
			ConceptComplex conceptComplex = (ConceptComplex) concept;
			conceptComplex.setHandler(extras.getHandler());
		}

		if (concept instanceof ConceptNumeric) {
			ConceptNumeric numeric = (ConceptNumeric) concept;

			numeric.setHiAbsolute(extras.getHiAbsolute());
			numeric.setHiCritical(extras.getHiCritical());
			numeric.setHiNormal(extras.getHiNormal());
			numeric.setLowAbsolute(extras.getLowAbsolute());
			numeric.setLowCritical(extras.getLowCritical());
			numeric.setLowNormal(extras.getLowNormal());
			numeric.setUnits(extras.getUnits());
			setAllowDecimal(numeric, extras);
		}

		if (extras.getIsSet() != null) {
			concept.setSet(extras.getIsSet() == 1);
		}

		concept.setRetired(oclConcept.isRetired());
		if (oclConcept.isRetired()) {
			concept.setRetireReason("Retired in OCL");
		} else {
			concept.setRetireReason(null);
			concept.setRetiredBy(null);
		}

		convertConceptNameTypes(oclConcept);

		voidNamesRemovedFromOcl(concept, oclConcept);

		updateOrAddNamesFromOcl(concept, oclConcept);

		removeDescriptionsRemovedFromOcl(concept, oclConcept);

		addDescriptionsFromOcl(concept, oclConcept);

		return concept;
	}

	private void convertConceptDatatypes(OclConcept oclConcept) {
		if (StringUtils.equalsIgnoreCase("None", oclConcept.getDatatype())) {
			oclConcept.setDatatype("N/A");
		}
	}

	private void convertConceptNameTypes(OclConcept oclConcept) {
		for (OclConcept.Name oclName: oclConcept.getNames()) {
			if (oclName.getNameType() != null) {
				switch (oclName.getNameType().toUpperCase(Locale.ENGLISH).replaceAll("[\\-_]", " ")) {
					case "FULLY SPECIFIED":
						oclName.setNameType("FULLY_SPECIFIED");
						break;
					case "INDEX TERM":
						oclName.setNameType("INDEX_TERM");
						break;
					case "SHORT":
						oclName.setNameType("SHORT");
						break;
					case "NONE":
					case "SYNONYM":
						oclName.setNameType(null);
						break;
					default:
						log.warn("Found unknown name type {} for concept {}. Treating as synonym.",
								oclName.getNameType(), oclConcept.getVersionUrl());
						oclName.setNameType(null);
						break;
				}
			}
		}
	}

	private void setAllowDecimal(ConceptNumeric numeric, Extras extras) {
		Boolean allowDecimal = extras.getAllowDecimal();
		if (allowDecimal == null) {
			allowDecimal = extras.getPrecise();
			if (allowDecimal != null) {
				log.warn("precise is deprecated, use allow_decimal instead");
			}
		}
		if (allowDecimal == null) {
			allowDecimal = false;
		}
		Utils.setAllowDecimal(numeric, allowDecimal);
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
	public Item saveMapping(final CacheService cacheService, final Import update, final OclMapping oclMapping) throws ImportException {
		try {
				Item oldMappingItem = importService.getLastSuccessfulItemByUrl(oclMapping.getUrl());
				if (oldMappingItem != null && isMappingUpToDate(oldMappingItem, oclMapping)) {
					return new Item(update, oclMapping, ItemState.UP_TO_DATE);
				}

				final Item item;
				Item fromItem;
				Concept fromConcept = null;
				if (!StringUtils.isBlank(oclMapping.getFromConceptUrl())) {
					fromItem = importService.getLastSuccessfulItemByUrl(oclMapping.getFromConceptUrl());
					if (fromItem != null) {
						fromConcept = cacheService.getConceptByUuid(fromItem.getUuid());
					}
					
					if (fromConcept == null) {
						String source = oclMapping.getFromSourceName();
						String code = oclMapping.getFromConceptCode();
						fromConcept = cacheService.getConceptWithSameAsMapping(source, code);
					}

					if (fromConcept == null) {
						throw new SavingException("Cannot create mapping from concept with URL " + oclMapping.getFromConceptUrl()
								+ ", because the concept has not been imported");
					}
				}

				if (MapType.Q_AND_A.equals(oclMapping.getMapType()) || MapType.SET.equals(oclMapping.getMapType())) {
					if (fromConcept == null) {
						throw new SavingException("Cannot create mapping for " + oclMapping.getUrl() + " as no from concept is"
								+ " defined");
					}

					Item toItem;
					Concept toConcept = null;
					if (!StringUtils.isBlank(oclMapping.getToConceptUrl())) {
						toItem = importService.getLastSuccessfulItemByUrl(oclMapping.getToConceptUrl());
						if (toItem != null) {
							toConcept = cacheService.getConceptByUuid(toItem.getUuid());
						}
						
						if (toConcept == null) {
							String source = oclMapping.getToSourceName();
							String code = oclMapping.getToConceptCode();
							toConcept = cacheService.getConceptWithSameAsMapping(source, code);
						}

						if (toConcept == null) {
							throw new SavingException("Cannot create mapping to concept with URL "
									+ oclMapping.getToConceptUrl() + ", because the concept has not been imported");
						}
					}

					if (toConcept == null) {
						throw new SavingException("Cannot create mapping " + oclMapping.getUrl() + " as no to concept is defined");
					}

					if (oclMapping.getMapType().equals(MapType.Q_AND_A)) {
						item = updateOrAddAnswersFromOcl(update, oclMapping, fromConcept, toConcept);
					} else {
						if (!fromConcept.getSet()) {
						    fromConcept.setSet(true);
						}
						item = updateOrAddSetMembersFromOcl(update, oclMapping, fromConcept, toConcept);
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
								toSource.setUuid(version5Uuid("source/" + oclMapping.getToSourceName()).toString());
								conceptService.saveConceptSource(toSource);
							}
						}
					}

					String mapTypeName = oclMapping.getMapType().replace("-", "_");
					ConceptMapType mapType = cacheService.getConceptMapTypeByName(mapTypeName);
					if (mapType == null) {
						synchronized (CREATE_MAP_TYPE_LOCK) {
							mapType = new ConceptMapType();
							mapType.setName(mapTypeName);
							mapType.setDescription("Imported from " + oclMapping.getUrl());
							mapType.setUuid(version5Uuid("mapType/" + mapTypeName).toString());
							conceptService.saveConceptMapType(mapType);
						}
					}

					if (fromConcept != null) {

						// Ensure a concept reference term exists that matches the passed in source/code
						ConceptReferenceTerm term;
						synchronized (CREATE_CONCEPT_REFERENCE_TERM_LOCK) {
							term = conceptService.getConceptReferenceTermByCode(oclMapping.getToConceptCode(), toSource);
							if (term == null) {
								term = new ConceptReferenceTerm();
								term.setConceptSource(toSource);
								term.setCode(oclMapping.getToConceptCode());
								conceptService.saveConceptReferenceTerm(term);
							}
							else {
								// If that term was previously retired, unretire it
								if (BooleanUtils.isTrue(term.getRetired())) {
									if (!BooleanUtils.isTrue(oclMapping.getRetired())) {
										term.setRetired(false);
										term.setRetiredBy(null);
										term.setDateRetired(null);
										term.setRetireReason(null);
										conceptService.saveConceptReferenceTerm(term);
									}
								}
							}
						}

						// Track what kind of change the incoming item state represents
						ItemState state;

						// Get any existing ConceptMap entry by uuid
						ConceptMap conceptMap = importService.getConceptMapByUuid(oclMapping.getExternalId());

						// If we find an existing Map by uuid, update it to match the passed in mapping
						if (conceptMap != null) {
							if (!conceptMap.getConcept().equals(fromConcept)) {
								Concept previousConcept = conceptMap.getConcept();
								previousConcept.removeConceptMapping(conceptMap);
								importService.updateConceptWithoutValidation(previousConcept);
								fromConcept.addConceptMapping(conceptMap);
							}
							conceptMap.setConceptReferenceTerm(term);
							conceptMap.setConceptMapType(mapType);
						}
						// If we don't find an existing Map by uuid, look for an exact match on the Concept
						else {
							for (ConceptMap existingMap : fromConcept.getConceptMappings()) {
								if (existingMap.getConceptReferenceTerm().equals(term)) {
									if (existingMap.getConceptMapType().equals(mapType)) {
										conceptMap = existingMap;
									}
								}
							}
						}

						// If we found an existing mapping, determine whether to retire or update
						if (conceptMap != null) {
							if (Boolean.TRUE.equals(oclMapping.getRetired())) {
								state = ItemState.RETIRED;
								fromConcept.removeConceptMapping(conceptMap);
							} else {
								state = ItemState.UPDATED;
							}
						}
						// Otherwise, if no existing mapping is found, create if the incoming mapping is not retired
						else {
							if (BooleanUtils.isNotTrue(oclMapping.getRetired())) {
								conceptMap = new ConceptMap();
								if (oclMapping.getExternalId() != null) {
									conceptMap.setUuid(oclMapping.getExternalId());
								} else {
									conceptMap.setUuid(version5Uuid(oclMapping.getUrl()).toString());
								}
								conceptMap.setConceptReferenceTerm(term);
								conceptMap.setConceptMapType(mapType);
								fromConcept.addConceptMapping(conceptMap);
								state = ItemState.ADDED;
							}
							else {
								// If the incoming mapping is retired, and no existing mapping is found, up to date
								state = ItemState.UP_TO_DATE;
							}
						}

						item = new Item(update, oclMapping, state);
						if (state != ItemState.UP_TO_DATE) {
							importService.updateConceptWithoutValidation(fromConcept);
						}
					} else {
						throw new SavingException("Mapping " + oclMapping.getUrl() + " is not supported");
					}
				}
				return item;
		} catch (Exception e) {
			throw new ImportException("Cannot save mapping " + oclMapping.getUrl(), e);
		}
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
		boolean updateDatesMatch = OpenmrsUtil.nullSafeEquals(oldItem.getUpdatedOn(), newMapping.getUpdatedOn());
		boolean retiredMatch = newMapping.isRetired() == (oldItem.getState() == ItemState.RETIRED);
		return updateDatesMatch && retiredMatch;
	}

	Item updateOrAddSetMembersFromOcl(Import update, OclMapping oclMapping, Concept set, Concept member) {
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
					conceptSet.setSortWeight(getSortWeightForMapping(oclMapping, 1.0));
				}

				break;
			}
		}

		if (!found && BooleanUtils.isNotTrue(oclMapping.getRetired())) {
			ConceptSet conceptSet = new ConceptSet();
			conceptSet.setConceptSet(set);
			conceptSet.setConcept(member);
			conceptSet.setUuid(oclMapping.getExternalId());
			conceptSet.setSortWeight(getSortWeightForMapping(oclMapping, 1.0));
			set.getConceptSets().add(conceptSet);
			item = new Item(update, oclMapping, ItemState.ADDED);
		} else {
			item = new Item(update, oclMapping, ItemState.UP_TO_DATE);
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
					conceptAnswer.setSortWeight(getSortWeightForMapping(oclMapping, null));
				}

				break;
			}
		}

		if (!found && BooleanUtils.isNotTrue(oclMapping.getRetired())) {
			ConceptAnswer conceptAnswer = new ConceptAnswer(answer);
			conceptAnswer.setUuid(oclMapping.getExternalId());
			conceptAnswer.setSortWeight(getSortWeightForMapping(oclMapping, null));
			question.addAnswer(conceptAnswer);
			item = new Item(update, oclMapping, ItemState.ADDED);
		} else {
			item = new Item(update, oclMapping, ItemState.UP_TO_DATE);
		}

		return item;
	}

	private Double getSortWeightForMapping(OclMapping oclMapping, Double defaultIfUndefined) {
		Double sortWeight = oclMapping.getSortWeight();
		if (sortWeight != null) {
			return sortWeight;
		}
		
		return defaultIfUndefined;
	}

	private void updateOrAddNamesFromOcl(Concept concept, OclConcept oclConcept) {

		for (OclConcept.Name oclName : sortedNames(oclConcept.getNames())) {
			ConceptNameType oclNameType;
			try {
				oclNameType = StringUtils.isNotBlank(oclName.getNameType()) ?
						ConceptNameType.valueOf(oclName.getNameType()) : null;
			} catch (IllegalArgumentException e) {
				oclNameType = null;
			}

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
				if (StringUtils.isNotEmpty(oclName.getExternalId())) {
					name.setUuid(oclName.getExternalId());
				} else {
					name.setUuid(version5Uuid(oclConcept.getUrl() + "/names/" + oclName.getUuid()).toString());
				}
				name.setConceptNameType(oclNameType);
				name.setLocalePreferred(oclName.isLocalePreferred());
				concept.addName(name);
			}
		}
	}

	private boolean hasExistingPreferredNameInExactLocale(Concept concept, Locale locale) {
		for (ConceptName cn : concept.getNames()) {
			if (BooleanUtils.isTrue(cn.getLocalePreferred()) && cn.getLocale().equals(locale)) {
				return true;
			}
		}
		return false;
	}

	private List<OclConcept.Name> sortedNames(List<OclConcept.Name> names) {

        // sort all FQN to the front of this, as the OpenMRS API expected FQN to be added first
        List<OclConcept.Name> sortedNames = new ArrayList<OclConcept.Name>(names);

        Collections.sort(sortedNames, new Comparator<OclConcept.Name>() {
            @Override
            public int compare(OclConcept.Name name0, OclConcept.Name name1) {
                ConceptNameType nameType0 = StringUtils.isNotBlank(name0.getNameType()) ? ConceptNameType.valueOf(name0.getNameType())
                        : null;
                ConceptNameType nameType1 = StringUtils.isNotBlank(name1.getNameType()) ? ConceptNameType.valueOf(name1.getNameType())
                        : null;
                if (nameType0 != null && nameType0.equals(ConceptNameType.FULLY_SPECIFIED)) {
                    return -1;
                }
                else if (nameType1 != null && nameType1.equals(ConceptNameType.FULLY_SPECIFIED)) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
        });

        return sortedNames;
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
		if (oclConcept.getDescriptions() != null) {
			for (Description oclDescription : oclConcept.getDescriptions()) {
                if (StringUtils.isBlank(oclDescription.getDescription())) {
                    continue;
                }

                boolean descriptionFound = false;
                for (ConceptDescription description : concept.getDescriptions()) {
                    if (isMatch(oclDescription, description)) {
                        //Let's make sure all is the same
                        description.setDescription(oclDescription.getDescription());
                        description.setLocale(oclDescription.getLocale());
                        descriptionFound = true;
                        break;
                    }
                }

                if (!descriptionFound) {
                    ConceptDescription description = new ConceptDescription(oclDescription.getDescription(),
                            oclDescription.getLocale());
	                if (oclDescription.getExternalId() != null) {
		                description.setUuid(oclDescription.getExternalId());
	                } else {
		                description.setUuid(version5Uuid(oclConcept.getUrl() + "/descriptions/" + oclDescription.getUuid()).toString());
	                }
                    concept.addDescription(description);
                }
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
		if (oclName == null) {
			return false;
		}

		if (name == null) {
			return false;
		}

		if (oclName.getExternalId() == null) {
			if (name.getUuid() == null) {
				return name.getLocale().equals(oclName.getLocale()) &&
						name.getName().equals(oclName.getName()) &&
						name.getConceptNameType().toString().equals(oclName.getNameType());
			}

			return false;
		}

		if (name.getUuid() == null) {
			return name.getLocale().equals(oclName.getLocale()) &&
					name.getName().equals(oclName.getName()) &&
					name.getConceptNameType().toString().equals(oclName.getNameType());
		}

		return name.getUuid().equals(oclName.getExternalId());
	}

	public boolean isMatch(OclConcept.Description oclDescription, ConceptDescription description) {
		if (oclDescription == null) {
			return false;
		}

		if (description == null) {
			return false;
		}

		if (oclDescription.getExternalId() == null) {
			if (description.getUuid() == null) {
				return description.getLocale().equals(oclDescription.getLocale()) &&
						description.getDescription().equals(oclDescription.getDescription());
			}

			return false;
		}

		return description.getUuid().equals(oclDescription.getExternalId());
	}
}
