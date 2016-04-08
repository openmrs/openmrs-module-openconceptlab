/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.updater;

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
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclMapping;

public class ImportRunner implements Runnable {
	
	private final Log log = LogFactory.getLog(getClass());
	
	Importer importer;
	
	UpdateService updateService;
	
	Update update;
	
	List<OclConcept> oclConcepts;
	
	List<OclMapping> oclMappings;
	
	CacheService cacheService;
	
	public ImportRunner(Importer importer, CacheService cacheService, UpdateService updateService, Update update) {
		this.importer = importer;
		this.updateService = updateService;
		this.update = update;
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
				update = updateService.getUpdate(update.getUpdateId());
				
				if (oclConcepts != null) {
					List<Item> items = new ArrayList<Item>();
					
					for (OclConcept oclConcept : oclConcepts) {
						Item item = null;
						try {
							item = importer.importConcept(cacheService, update, oclConcept);
							log.info("Imported concept " + oclConcept);
						}
						catch (Throwable e) {
							log.error("Failed to import concept " + oclConcept, e);
							Context.clearSession();
							cacheService.clearCache();

							item = new Item(update, oclConcept, ItemState.ERROR);
							item.setErrorMessage(Updater.getErrorMessage(e));
						} finally {
							if(item.getState() != ItemState.ALREADY_UP_TO_DATE) {
								items.add(item);
							}
						}
	                }
					updateService.saveItems(items);
				}
				
				if (oclMappings != null) {
					List<Item> items = new ArrayList<Item>();
					
					for (OclMapping oclMapping : oclMappings) {
						Item item = null;
						try {
							item = importer.importMapping(cacheService, update, oclMapping);
							log.info("Imported mapping " + oclMapping);
						}
						catch (Throwable e) {
							log.error("Failed to import mapping " + oclMapping, e);
							Context.clearSession();
							cacheService.clearCache();
														
							item = new Item(update, oclMapping, ItemState.ERROR);
							item.setErrorMessage(Updater.getErrorMessage(e));
						} finally {
							if(item.getState() != ItemState.ALREADY_UP_TO_DATE) {
								items.add(item);
							}
						}
                    }
					
					updateService.saveItems(items);
				}
			}
		}, OpenConceptLabActivator.getDaemonToken());
	}
}
