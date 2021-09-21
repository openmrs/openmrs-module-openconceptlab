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

import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.openconceptlab.CacheService;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.OpenConceptLabActivator;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImportTask implements Runnable {
	
	private final Logger log = LoggerFactory.getLogger(ImportTask.class);
	
	private final Saver saver;
	
	private final ImportService updateService;
	
	private final Long importId;
	
	private List<OclConcept> oclConcepts;
	
	private List<OclMapping> oclMappings;
	
	private final CacheService cacheService;
	
	public ImportTask(Saver saver, CacheService cacheService, ImportService updateService, Import anImport) {
		this.saver = saver;
		this.updateService = updateService;
		this.importId = anImport.getImportId();
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
				Import anImport = updateService.getImport(importId);
				
				if (oclConcepts != null) {
					List<Item> items = new ArrayList<>(oclConcepts.size());
					
					for (OclConcept oclConcept : oclConcepts) {
						Item item = null;
						try {
							item = saver.saveConcept(cacheService, anImport, oclConcept);
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

						if (oclConcept.getSource() != null) {
							if (oclMappings == null) {
								oclMappings = new ArrayList<>(oclConcepts.size());
							}

							OclMapping sourceMapping = new OclMapping();
							sourceMapping.setUrl(oclConcept.getSourceUrl() + "mappings/custom/" + oclConcept.getExternalId());
							sourceMapping.setFromConceptUrl(oclConcept.getUrl());
							sourceMapping.setToSourceName(oclConcept.getSource());
							sourceMapping.setToConceptCode(oclConcept.getId());
							sourceMapping.setMapType("SAME-AS");
							sourceMapping.setUpdatedOn(new Date());

							oclMappings.add(sourceMapping);
						}
	                }
					updateService.saveItems(items);
				}
				
				if (oclMappings != null) {
					List<Item> items = new ArrayList<>(oclMappings.size());
					
					for (OclMapping oclMapping : oclMappings) {
						Item item = null;
						try {
							item = saver.saveMapping(cacheService, anImport, oclMapping);
							log.info("Imported mapping " + oclMapping);
						} catch (SavingException e) {
							log.error("Failed to save mapping " + oclMapping, e);
							Context.clearSession();
							cacheService.clearCache();

							item = new Item(anImport, oclMapping, ItemState.ERROR, e.getMessage());
						} catch (Throwable e) {
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
