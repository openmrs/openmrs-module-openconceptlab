package org.openmrs.module.openconceptlab;

import org.openmrs.Concept;
import org.springframework.transaction.annotation.Transactional;

public interface OclConceptService {
	
	/**
	 * A version of {@link org.openmrs.api.ConceptService#getConceptByMapping(String, String)}
	 * which returns concepts that are marked as the "SAME-AS" an existing concept
	 *
	 * @param source the name of the source to check
	 * @param code the identifier of the concept
	 * @return the concept that is semantically the same as the requested concept or null if
	 *  one couldn't be found.
	 */
	@Transactional(readOnly = true)
	Concept getConceptWithSameAsMapping(String code, String source);
	
}
