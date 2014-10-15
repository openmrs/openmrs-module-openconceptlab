package org.openmrs.module.openconceptlab;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConceptMapper {
	
	@Autowired
	@Qualifier("conceptService")
	ConceptService conceptService;
	
	@Autowired
	UpdateService updateService;
	
	private Map<String, OclConceptQueued> queued = new HashMap<String, ConceptMapper.OclConceptQueued>();
	
	@Transactional
	public void map(Update update, OclConcept oclConcept) {
		Concept concept = conceptService.getConceptByUuid(oclConcept.getUuid());
		if (concept == null) {
			concept = new Concept();
		}
		
		ConceptClass conceptClass = conceptService.getConceptClassByName(oclConcept.getConceptClass());
		if (conceptClass == null) {
			throw new IllegalStateException("Missing concept class: " + oclConcept.getConceptClass());
		}
		concept.setConceptClass(conceptClass);
		
		ConceptDatatype datatype = conceptService.getConceptDatatypeByName(oclConcept.getDatatype());
		if (datatype == null) {
			throw new IllegalStateException("Datatype is not supported: " + oclConcept.getDatatype());
		}
		concept.setDatatype(datatype);
		
		concept.setRetired(oclConcept.isRetired());
		
		for (OclConcept.Name oclName : oclConcept.getNames()) {
			ConceptNameType oclNameType = oclName.getNameType() != null ? ConceptNameType.valueOf(oclName.getNameType()) : null;
			
			boolean nameFound = false;
			for (ConceptName name : concept.getNames()) {		
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
				
				//TBD: work in progress
			}
		}
	}
	
	public boolean isMatch(OclConcept.Name oclName, ConceptName name) {
		return new EqualsBuilder().append(name.getLocale(), oclName.getLocale()).append(name.getName(), oclName.getName()).isEquals();
	}
	
	
	public static class OclConceptQueued {
		
		private OclConcept oclConcept;
		
		private int missingDependencies = 0;
		
		public OclConceptQueued(OclConcept oclConcept, int missingDependencies) {
			this.oclConcept = oclConcept;
			if (missingDependencies <= 0) {
				throw new IllegalArgumentException("Missing dependencies must be greater than 0");
			}
			this.missingDependencies = missingDependencies;
		}
		
		public boolean satisfyDependency() {
			missingDependencies--;
			if (missingDependencies > 0) {
				return false;
			} else if (missingDependencies == 0) {
				return true;
			} else {
				throw new IllegalStateException("Missing dependencies must not be lower than 0");
			}
		}
		
		public OclConcept getOclConcept() {
			return oclConcept;
		}
	}
}
