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

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.Utils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Controller to run previous updates
 */
public class PreviousUpdatesFragmentController {
			public void controller(FragmentModel model,
								   @SpringBean UpdateService service
			){
				List<Update> allUpdates = service.getUpdatesInOrder(0, 20);
				List<UpdateSummary> summaryList = new ArrayList<UpdateSummary>();
				int duration = 0;

				for(Update update: allUpdates) {
					if(update != null) {
						if (!update.isStopped()) {
							continue;
						}
						Set<ItemState> states = new HashSet<ItemState>();
						states.add(ItemState.ERROR);

						Integer errors = service.getUpdateItemsCount(update, states);
						Integer totalItems =  service.getUpdateItemsCount(update, new HashSet<ItemState>());
						//loop through each item object to count error
						duration = Utils.dateDifference(update.getLocalDateStarted(), update.getLocalDateStopped(), TimeUnit.MINUTES).intValue();String status;

						if( errors > 0) {
							status = errors + " errors";
						} else if (!StringUtils.isBlank(update.getErrorMessage())){
							status = update.getErrorMessage();
						}
						else {
							status = totalItems + " items updated";
						}
						summaryList.add(new UpdateSummary(update.getUpdateId(), Utils.formatDateAuto(update.getLocalDateStarted()), duration, totalItems,status));
					}
				}
				model.addAttribute("summaryList", summaryList);
			}
	class UpdateSummary{


		private Long updateId;
		private String startDate;
		private Integer duration;
		private String status;


		public UpdateSummary(Long updateId, String startDate, Integer duration, Integer items, String status) {
			this.updateId = updateId;
			this.startDate = startDate;
			this.duration = duration;
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

		public Integer getDuration() {
			return duration;
		}

		public void setDuration(Integer duration) {
			this.duration = duration;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}


	}
}
