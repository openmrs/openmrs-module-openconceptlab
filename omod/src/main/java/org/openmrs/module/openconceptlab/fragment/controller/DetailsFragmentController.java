/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.openconceptlab.fragment.controller;

import org.openmrs.Concept;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.State;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.Utils;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

/**
 * Fragment controller to list all the errors in the last update.
 */
public class DetailsFragmentController {
			public void controller(FragmentModel model,
								   @SpringBean("openconceptlab.updateService") UpdateService service,
								   @FragmentParam(value ="updateId", required = false) Long updateId
									){
				//fetch  update
				Update fetchedUpdate = service.getUpdate(updateId);
				Date upgradeStartDate;
				List<Details> detailsList = new ArrayList<Details>();
				ConceptService conceptService = Context.getConceptService();
				Concept concept;
				Date upgradeStopDate;
				Long timeTakenForUpgrade;
				String duration = "";
				Integer minutes;
				int seconds;
				List<Item> errorItems = new ArrayList<Item>();
				//go through a set of items and find the number of items
				if(fetchedUpdate != null) {
					//get the start date
					upgradeStartDate = fetchedUpdate.getLocalDateStarted();
					upgradeStopDate = fetchedUpdate.getLocalDateStopped();
					SortedSet<Item> itemsUpdated = fetchedUpdate.getItems();
					//loop through and find those with errors
					for (Item item : itemsUpdated) {
						if (item.getState().equals(State.ERROR)) {
							errorItems.add(item);
						}
					concept = conceptService.getConceptByUuid(item.getUuid());
					detailsList.add(new Details(item, concept));

					}
					//calculate the time it take for the upgrade
					timeTakenForUpgrade = Utils.dateDifference(upgradeStartDate, upgradeStopDate, TimeUnit.SECONDS);
					if (timeTakenForUpgrade < 60) {
						duration = timeTakenForUpgrade+" seconds";
					}
					else {
						minutes = (int) (timeTakenForUpgrade/60);
						seconds = (int) (timeTakenForUpgrade%60);
						duration = minutes+" minutes"+"  "+seconds+" seconds";
					}

					model.addAttribute("allErrorItems", errorItems.size());
					model.addAttribute("startDate", Utils.formatedDate(upgradeStartDate));
					model.addAttribute("timeStarted", Utils.formatTime(upgradeStartDate));
					model.addAttribute("duration", duration);
					model.addAttribute("allItemsUpdatedSize", itemsUpdated.size() - errorItems.size());
					model.addAttribute("allItemsUpdated", detailsList);
				}

	}
	class Details {

		private Long updateId;
		private String type;
		private String name;
		private String description;
		private String status;
		private String uuid;
		private String versionUrl;
		private Integer conceptId;
		public Details(Item item, Concept concept) {
			this.updateId = item.getUpdate().getUpdateId();
			this.type = item.getType();
			if (concept != null) {
				this.name = concept.getName().getName();
				this.description = concept.getDescription().getDescription();
				this.conceptId = concept.getConceptId();
			}
			if (State.ERROR.equals(item.getState())) {
				this.status = item.getErrorMessage();
			} else {
				this.status = item.getState().name();
			}
			this.uuid = item.getUuid();
			this.versionUrl = item.getVersionUrl();
		}
		public Integer getConceptId() {
			return conceptId;
		}

		public void setConceptId(Integer conceptId) {
			this.conceptId = conceptId;
		}
		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
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

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
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
