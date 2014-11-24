package org.openmrs.module.openconceptlab.updater;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.openmrs.module.openconceptlab.client.OclConcept;

public class ImportQueue {
	
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
}
