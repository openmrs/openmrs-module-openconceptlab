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
package org.openmrs.module.openconceptlab.fragment.controller.status;

import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.State;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.Utils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Controller to run previous updates
 */
public class PreviousUpdatesFragmentController {
			public void controller(FragmentModel model,
								   @SpringBean UpdateService service
			){
				List<Update> allUpdates = service.getUpdatesInOrder();
				List<UpdateSummary> summaryList = new ArrayList<UpdateSummary>();
				SortedSet<Item> items;
				Long duration = null;

				for(Update update: allUpdates) {
					if(update != null) {
						int errors = 0;
						items = new TreeSet<Item>(update.getItems());
						//loop through each item object to count errors
						for( Item item : items){
							if (item != null) {
								if (item.getState().equals(State.ERROR)) {
									errors++;
								}
								duration = Utils.dateDifference(update.getLocalDateStarted(), update.getLocalDateStopped(), TimeUnit.MINUTES);
							}
						}
						String error;
						if( errors > 0) {
							error = errors+" errors";
						}
						else {
							error = "Ok";
						}
						summaryList.add(new UpdateSummary(update.getUpdateId(), Utils.formatDateAuto(update.getLocalDateStarted()), duration, items.size(),error));
					}
				}
				model.addAttribute("summaryList", summaryList);
			}
	class UpdateSummary{


		private Long updateId;
		private String startDate;
		private Long duration;
		private Integer items;
		private String status;


		public UpdateSummary(Long updateId, String startDate, Long duration, Integer items, String status) {
			this.updateId = updateId;
			this.startDate = startDate;
			this.duration = duration;
			this.items = items;
			this.status = status;
		}

		public Long getUpdateId() {
			return updateId;
		}

		public void setUpdateId(Long updateId) {
			this.updateId = updateId;
		}

		public String getStartDate() {
			return startDate;
		}

		public void setStartDate(String startDate) {
			this.startDate = startDate;
		}

		public Long getDuration() {
			return duration;
		}

		public void setDuration(Long duration) {
			this.duration = duration;
		}

		public Integer getItems() {
			return items;
		}

		public void setItems(Integer items) {
			this.items = items;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}


	}
}
