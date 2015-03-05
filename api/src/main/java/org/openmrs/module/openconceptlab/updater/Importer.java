package org.openmrs.module.openconceptlab.updater;

import java.util.Iterator;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.State;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclConcept.Description;
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
		Concept concept = conceptService.getConceptByUuid(oclConcept.getUuid());
		if (concept == null) {
			concept = new Concept();
			concept.setUuid(oclConcept.getUuid());
		}
		
		final Item item;
		if (concept.getId() == null) {
			item = new Item(update, oclConcept, State.ADDED);
		} else if (!concept.isRetired() && oclConcept.isRetired()) {
			item = new Item(update, oclConcept, State.RETIRED);
		} else if (concept.isRetired() && !oclConcept.isRetired()) {
			item = new Item(update, oclConcept, State.UNRETIRED);
		} else {
			item = new Item(update, oclConcept, State.UPDATED);
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
			throw new ImportException("Cannot save concept with UUID " + oclConcept.getUuid(), e);
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
		return new EqualsBuilder().append(name.getLocale(), oclName.getLocale()).append(name.getName(), oclName.getName())
		        .isEquals();
	}
	
	public boolean isMatch(OclConcept.Description oclDescription, ConceptDescription description) {
		return new EqualsBuilder().append(description.getLocale(), oclDescription.getLocale())
		        .append(description.getDescription(), oclDescription.getDescription()).isEquals();
	}
}
