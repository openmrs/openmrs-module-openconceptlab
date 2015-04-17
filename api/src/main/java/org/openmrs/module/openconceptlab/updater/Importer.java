package org.openmrs.module.openconceptlab.updater;

import java.util.Iterator;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSet;
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclConcept.Description;
import org.openmrs.module.openconceptlab.client.OclMapping;
import org.openmrs.module.openconceptlab.client.OclMapping.MapType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("openconceptlab.importer")
public class Importer {
	
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
	 * @should void names from concept
	 * @should add new descriptions to concept
	 * @should void descriptions from concept
	 * @should retire concept
	 * @should unretire concept
	 * @should update datatype
	 * @should update concept class
	 * @should fail if concept class missing
	 * @should fail if datatype missing
	 */
	@Transactional
	public Item importConcept(Update update, OclConcept oclConcept) throws ImportException {
		Concept concept = conceptService.getConceptByUuid(oclConcept.getExternalId());
		if (concept == null) {
			concept = new Concept();
			concept.setUuid(oclConcept.getExternalId());
		}
		
		final Item item;
		if (concept.getId() == null) {
			item = new Item(update, oclConcept, ItemState.ADDED);
		} else if (!concept.isRetired() && oclConcept.isRetired()) {
			item = new Item(update, oclConcept, ItemState.RETIRED);
		} else if (concept.isRetired() && !oclConcept.isRetired()) {
			item = new Item(update, oclConcept, ItemState.UNRETIRED);
		} else {
			item = new Item(update, oclConcept, ItemState.UPDATED);
		}
		
		ConceptClass conceptClass = conceptService.getConceptClassByName(oclConcept.getConceptClass());
		if (conceptClass == null) {
			throw new ImportException("Concept class named '" + oclConcept.getConceptClass() + "' is missing");
		}
		concept.setConceptClass(conceptClass);
		
		ConceptDatatype datatype = conceptService.getConceptDatatypeByName(oclConcept.getDatatype());
		if (datatype == null) {
			throw new ImportException("Datatype " + oclConcept.getDatatype() + " is not supported in OpenMRS");
		}
		
		try {
			concept.setDatatype(datatype);
			
			concept.setRetired(oclConcept.isRetired());
			
			voidNamesRemovedFromOcl(concept, oclConcept);
			
			updateOrAddNamesFromOcl(concept, oclConcept);
			
			removeDescriptionsRemovedFromOcl(concept, oclConcept);
			
			addDescriptionsFromOcl(concept, oclConcept);
			
			conceptService.saveConcept(concept);
		}
		catch (Exception e) {
			throw new ImportException("Cannot save concept with UUID " + concept.getUuid(), e);
		}
		
		return item;
	}
	
	/**
	 * 
	 * @param update
	 * @param oclMapping
	 * @return
	 * 
	 * @should add concept answer
	 * @should add concept set member
	 * @should remove concept answer
	 * @should remove concept set member
	 * @should add concept mapping and term
	 * @should add concept mapping and unretire term
	 * @should remove concept mapping and retire term
	 */
	@Transactional
	public Item importMapping(Update update, OclMapping oclMapping) {
		Item item = null;
		
		Item fromItem = null;
		if (oclMapping.getFromConceptUrl() != null) {
			fromItem = updateService.getLastSuccessfulItemByUrl(oclMapping.getFromConceptUrl());
		}
		Concept fromConcept = null;
		if (fromItem != null) {
			fromConcept = conceptService.getConceptByUuid(fromItem.getUuid());
		}
		
		Item toItem = null;
		if (oclMapping.getToConceptUrl() != null) {
			toItem = updateService.getLastSuccessfulItemByUrl(oclMapping.getToConceptUrl());
		}
		Concept toConcept = null;
		if (toItem != null) {
			toConcept = conceptService.getConceptByUuid(toItem.getUuid());
		}
		
		if (MapType.Q_AND_A.equals(oclMapping.getMapType()) || MapType.SET.equals(oclMapping.getMapType())) {
			if (fromConcept == null) {
				return new Item(update, oclMapping, ItemState.ERROR, "Concept (from) with URL "
				        + oclMapping.getFromConceptUrl() + " has not been imported");
			}

			if (toConcept == null) {
				return new Item(update, oclMapping, ItemState.ERROR, "Concept (to) with URL " + oclMapping.getToConceptUrl()
				        + " has not been imported");
			}
			
			if (oclMapping.getMapType().equals(MapType.Q_AND_A)) {
				item = updateOrAddAnswersFromOcl(update, oclMapping, fromConcept, toConcept);
			} else {
				item = updateOrAddSetMemebersFromOcl(update, oclMapping, fromConcept, toConcept);
			}
			
			conceptService.saveConcept(fromConcept);
		} else {			
			ConceptSource toSource = conceptService.getConceptSourceByName(oclMapping.getToSourceName());
			if (toSource == null) {
				toSource = new ConceptSource();
				toSource.setName(oclMapping.getToSourceName());
				conceptService.saveConceptSource(toSource);
			}
			
			String mapTypeName = oclMapping.getMapType().replace("-", "_");
			ConceptMapType mapType = conceptService.getConceptMapTypeByName(mapTypeName);
			if (mapType == null) {
				return new Item(update, oclMapping, ItemState.ERROR, "Map type " + mapTypeName
			        + " does not exist");
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
				}
				
				conceptService.saveConcept(fromConcept);
			}
		}
		return item;
	}

	ConceptReferenceTerm createOrUpdateConceptReferenceTerm(OclMapping oclMapping, ConceptMap conceptMap,
            ConceptSource toSource) {
	    ConceptReferenceTerm term = null;
	    if (conceptMap == null) {
	    	term = conceptService.getConceptReferenceTermByCode(oclMapping.getToSourceCode(), toSource);
	    } else {
	    	term = conceptMap.getConceptReferenceTerm();
	    }
	    
	    if (term == null) {
	    	term = new ConceptReferenceTerm();
	    }
	    
	    term.setConceptSource(toSource);
	    term.setCode(oclMapping.getToSourceCode());
	    
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
			for (ConceptName name : concept.getNames(false)) {
				if (isMatch(oclName, name)) {
					if (!ObjectUtils.equals(name.getConceptNameType(), oclNameType)) {
						name.setConceptNameType(oclNameType);
					}
					nameFound = true;
				}
			}
			
			if (!nameFound) {
				ConceptName conceptName = new ConceptName(oclName.getName(), oclName.getLocale());
				conceptName.setUuid(oclName.getExternalId());
				conceptName.setConceptNameType(oclNameType);
				concept.addName(conceptName);
			}
		}
	}
	
	void voidNamesRemovedFromOcl(Concept concept, OclConcept oclConcept) {
		for (ConceptName name : concept.getNames(false)) {
			boolean nameFound = false;
			for (OclConcept.Name oclName : oclConcept.getNames()) {
				if (isMatch(oclName, name)) {
					nameFound = true;
				}
			}
			if (!nameFound) {
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
					nameFound = true;
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
				}
			}
			if (!descriptionFound) {
				it.remove();
			}
		}
	}
	
	public boolean isMatch(OclConcept.Name oclName, ConceptName name) {
		return new EqualsBuilder().append(name.getUuid(), oclName.getExternalId()).isEquals()
		        || new EqualsBuilder().append(name.getLocale(), oclName.getLocale())
		                .append(name.getName(), oclName.getName()).isEquals();
	}
	
	public boolean isMatch(OclConcept.Description oclDescription, ConceptDescription description) {
		return new EqualsBuilder().append(description.getUuid(), oclDescription.getExternalId()).isEquals()
		        || new EqualsBuilder().append(description.getLocale(), oclDescription.getLocale())
		                .append(description.getDescription(), oclDescription.getDescription()).isEquals();
	}
}
