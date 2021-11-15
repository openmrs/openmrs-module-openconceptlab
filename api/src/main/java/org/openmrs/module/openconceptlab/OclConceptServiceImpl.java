package org.openmrs.module.openconceptlab;

import java.util.List;

import org.openmrs.Concept;
import org.openmrs.api.APIException;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.api.impl.BaseOpenmrsService;

public class OclConceptServiceImpl extends BaseOpenmrsService implements OclConceptService {
	
	private DbSessionFactory dbSessionFactory;
	
	@Override
	public Concept getDuplicateConceptByMapping(String conceptId, String source) {
		@SuppressWarnings("unchecked")
		List<Concept> concepts = dbSessionFactory.getCurrentSession().createQuery(
						"select c "
								+ "from ConceptMap as cm "
								+ "join cm.concept as c "
								+ "join cm.conceptReferenceTerm as crt "
								+ "where crt.conceptSource.name = :source "
								+ "and crt.code = :code "
								+ "and upper(cm.conceptMapType.name) = 'SAME-AS' "
								+ "and c.retired = false "
								+ "and crt.retired = false")
				.setParameter("code", conceptId)
				.setParameter("source", source)
				.list();
		
		
		
		if (concepts == null) {
			return null;
		}
		
		if (concepts.isEmpty()) {
			return null;
		}
		
		if (concepts.size() > 1) {
			throw new APIException("Multiple non-retired concepts found for mapping " + conceptId + " from source " + source);
		}
		
		return concepts.get(0);
	}
	
	public void setDbSessionFactory(DbSessionFactory dbSessionFactory) {
		this.dbSessionFactory = dbSessionFactory;
	}
}
