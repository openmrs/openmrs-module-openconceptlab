package org.openmrs.module.openconceptlab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.module.openconceptlab.OclConcept.Description;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportAgent {
	
	@Autowired
	@Qualifier("conceptService")
	ConceptService conceptService;
	
	@Autowired
	UpdateService updateService;
	
	public static class ImportQueue {
		
		private final Map<String, List<UnsatisfiedDependency>> missingDependencies = new HashMap<String, List<UnsatisfiedDependency>>();
		
		private final Queue<OclConcept> queued = new LinkedList<OclConcept>();
		
		public void addUnsatisfiedDependency(UnsatisfiedDependency unsatisfiedDependency) {
			for (String missingUuid : unsatisfiedDependency.getMissingUuids()) {
	            List<UnsatisfiedDependency> unsatisfiedDependencies = missingDependencies.get(missingUuid);
	            if (unsatisfiedDependencies == null) {
	            	unsatisfiedDependencies = new ArrayList<UnsatisfiedDependency>();
	            }
	            
	            unsatisfiedDependencies.add(unsatisfiedDependency);
	            missingDependencies.put(missingUuid, unsatisfiedDependencies);
            }
		}
		
		public void satisfyDependencies(OclConcept oclConcept) {
			List<UnsatisfiedDependency> unsatisfiedDependencies = missingDependencies.remove(oclConcept.getUuid());
			if (unsatisfiedDependencies != null) {
				for (UnsatisfiedDependency unsatisfiedDependency : unsatisfiedDependencies) {
					if (unsatisfiedDependency.satisfyDependency(oclConcept.getUuid())) {
						if (unsatisfiedDependency.isSatisfied()) {
							queued.offer(unsatisfiedDependency.getOclConcept());
						}
					}
				}
			}
		}
		
		public OclConcept poll() {
			return queued.poll();
		}
		
		public OclConcept peek() {
			return queued.peek();
		}
		
		public boolean isEmpty() {
			return queued.isEmpty();
		}
		
		public boolean offer(OclConcept oclConcept) {
			return queued.offer(oclConcept);
		}
	}
	
	public static class UnsatisfiedDependency {
		
		private OclConcept oclConcept;
		
		private Set<String> missingUuids = new HashSet<String>();
					
		public UnsatisfiedDependency(OclConcept oclConcept) {
			this.oclConcept = oclConcept;
		}
		
		public boolean addMissingDependency(String uuid) {
			return missingUuids.add(uuid);
		}
		
		public Set<String> getMissingUuids() {
			return missingUuids;
		}
		
		public boolean satisfyDependency(String uuid) {
			return missingUuids.remove(uuid);
		}
		
		public boolean isSatisfied() {
			return missingUuids.isEmpty();
		}
		
		public OclConcept getOclConcept() {
			return oclConcept;
		}
	}
	
	/**
	 * @param oclConcept
	 * @param importQueue
	 * @throws ImportException
	 * 
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
	public Item importConcept(ImportQueue importQueue) throws ImportException {
		OclConcept oclConcept = importQueue.poll();
		
		Concept concept = conceptService.getConceptByUuid(oclConcept.getUuid());
		if (concept == null) {
			concept = new Concept();
			concept.setUuid(oclConcept.getUuid());
		}
		
		final Item item;
		if (concept.getId() == null) {
			item = new Item(oclConcept, State.ADDED);
		} else if (!concept.isRetired() && oclConcept.isRetired()) {
			item = new Item(oclConcept, State.RETIRED);
		} else if (concept.isRetired() && !oclConcept.isRetired()) {
			item = new Item(oclConcept, State.UNRETIRED);
		} else {
			item = new Item(oclConcept, State.UPDATED);
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
			
			//TODO: implement answers, sets, mappings
			
			conceptService.saveConcept(concept);
			
			importQueue.satisfyDependencies(oclConcept);
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
