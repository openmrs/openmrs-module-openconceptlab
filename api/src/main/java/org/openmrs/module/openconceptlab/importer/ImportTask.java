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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.openconceptlab.CacheService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.OpenConceptLabActivator;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclMapping;

public class ImportTask implements Runnable {
	
	private final Log log = LogFactory.getLog(getClass());
	
	private Saver importer;
	
	private ImportService updateService;
	
	private Import anImport;
	
	private List<OclConcept> oclConcepts;
	
	private List<OclMapping> oclMappings;
	
	private CacheService cacheService;
	
	public ImportTask(Saver importer, CacheService cacheService, ImportService updateService, Import anImport) {
		this.importer = importer;
		this.updateService = updateService;
		this.anImport = anImport;
		this.cacheService = cacheService;
	}
	
    public void setOclConcepts(List<OclConcept> oclConcepts) {
	    this.oclConcepts = oclConcepts;
    }
    
    public void setOclMappings(List<OclMapping> oclMappings) {
	    this.oclMappings = oclMappings;
    }
	
	@Override
	public void run() {
		Daemon.runInDaemonThreadAndWait(new Runnable() {
			
			@Override
			public void run() {
				anImport = updateService.getImport(anImport.getImportId());
				
				if (oclConcepts != null) {
					List<Item> items = new ArrayList<Item>();
					
					for (OclConcept oclConcept : oclConcepts) {
						Item item = null;
						try {
							item = importer.saveConcept(cacheService, anImport, oclConcept);
							log.info("Imported concept " + oclConcept);
						}
						catch (Throwable e) {
							log.error("Failed to import concept " + oclConcept, e);
							Context.clearSession();
							cacheService.clearCache();

							item = new Item(anImport, oclConcept, ItemState.ERROR);
							item.setErrorMessage(Importer.getErrorMessage(e));
						} finally {
							items.add(item);
						}
	                }
					updateService.saveItems(items);
				}
				
				if (oclMappings != null) {
					List<Item> items = new ArrayList<Item>();
					
					for (OclMapping oclMapping : oclMappings) {
						Item item = null;
						try {
							item = importer.saveMapping(cacheService, anImport, oclMapping);
							log.info("Imported mapping " + oclMapping);
						}
						catch (Throwable e) {
							log.error("Failed to import mapping " + oclMapping, e);
							Context.clearSession();
							cacheService.clearCache();
														
							item = new Item(anImport, oclMapping, ItemState.ERROR);
							item.setErrorMessage(Importer.getErrorMessage(e));
						} finally {
							items.add(item);
						}
                    }
					
					updateService.saveItems(items);
				}
			}
		}, OpenConceptLabActivator.getDaemonToken());
	}
}
