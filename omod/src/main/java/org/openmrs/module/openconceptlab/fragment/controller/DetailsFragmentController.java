/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.fragment.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportProgress;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.Utils;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

/**
 * Fragment controller to list all the errors in the last update.
 */
public class DetailsFragmentController {
	
	public void controller(FragmentModel model, @SpringBean("openconceptlab.updateService") ImportService service,
	        @FragmentParam(value = "updateId", required = false) Long updateId,
	        @FragmentParam(value = "debug", required = false) Boolean debug) {
		//fetch  update
		Import fetchedUpdate = service.getImport(updateId);
		Date upgradeStartDate;
		List<Details> detailsList = new ArrayList<Details>();
		Date upgradeStopDate;
		Long timeTakenForUpgrade;
		String duration = "";
		//go through a set of items and find the number of items
		if (fetchedUpdate != null) {
			//get the start date
			upgradeStartDate = fetchedUpdate.getLocalDateStarted();
			upgradeStopDate = fetchedUpdate.getLocalDateStopped();
			Integer itemsUpdated = service.getImportItemsCount(fetchedUpdate, new HashSet<ItemState>());
			
			Set<ItemState> inError = new HashSet<ItemState>();
			inError.add(ItemState.ERROR);
			Integer errorsItems = service.getImportItemsCount(fetchedUpdate, inError);
			
			String baseUrl = service.getSubscription().getUrl();
			baseUrl = baseUrl.substring(0, baseUrl.indexOf("/"));
			
			List<Item> itemsUpdatedLimited = service.getImportItems(fetchedUpdate, 0, 1000, inError);
			for (Item item : itemsUpdatedLimited) {
				detailsList.add(new Details(item));
			}
			
			//calculate the time it take for the upgrade
			timeTakenForUpgrade = Utils.dateDifference(upgradeStartDate, upgradeStopDate, TimeUnit.SECONDS);
			duration = ImportProgress.convertSecondsToText(timeTakenForUpgrade);
			
			Set<ItemState> ignoredError = new HashSet<ItemState>();
			ignoredError.add(ItemState.IGNORED_ERROR);
			Integer ignoredErrorsCount = service.getImportItemsCount(fetchedUpdate, ignoredError);
			
			model.addAttribute("debug", debug);
			model.addAttribute("updateId", updateId);
			model.addAttribute("allErrorItems", errorsItems);
			model.addAttribute("startDate", Utils.formatedDate(upgradeStartDate));
			model.addAttribute("timeStarted", Utils.formatTime(upgradeStartDate));
			model.addAttribute("duration", duration);
			model.addAttribute("allItemsUpdatedSize", itemsUpdated - errorsItems);
			model.addAttribute("allItemsUpdated", detailsList);
			model.addAttribute("ignoredErrorsCount", ignoredErrorsCount);
		}
		
	}
	
	class Details {
		
		private Long updateId;
		
		private String type;
		
		private String status;
		
		private String uuid;
		
		private String versionUrl;
		
		public Details(Item item) {
			this.updateId = item.getAnImport().getImportId();
			this.type = item.getType().toString();
			if (ItemState.ERROR.equals(item.getState())) {
				this.status = "<pre style=\"font-size: 70%\">" + item.getErrorMessage() + "</pre>";
			} else {
				this.status = item.getState().name();
			}
			this.uuid = item.getUuid();
			this.versionUrl = item.getVersionUrl();
		}
		
		public Long getUpdateId() {
			return updateId;
		}
		
		public void setUpdateId(Long updateId) {
			this.updateId = updateId;
		}
		
		public String getType() {
			return type;
		}
		
		public void setType(String type) {
			this.type = type;
		}
		
		public String getStatus() {
			return status;
		}
		
		public void setStatus(String status) {
			this.status = status;
		}
		
		public String getUuid() {
			return uuid;
		}
		
		public void setUuid(String uuid) {
			this.uuid = uuid;
		}
		
		public String getVersionUrl() {
			return versionUrl;
		}
		
		public void setVersionUrl(String versionUrl) {
			this.versionUrl = versionUrl;
		}
	}
}
