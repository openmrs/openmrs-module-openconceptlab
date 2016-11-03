/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.fragment.controller.status;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
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
								   @SpringBean ImportService service
			){
				List<Import> allUpdates = service.getImportsInOrder(0, 20);
				List<UpdateSummary> summaryList = new ArrayList<UpdateSummary>();
				int duration = 0;

				for(Import update: allUpdates) {
					if(update != null) {
						if (!update.isStopped()) {
							continue;
						}
						Set<ItemState> states = new HashSet<ItemState>();
						states.add(ItemState.ERROR);

						Integer errors = service.getImportItemsCount(update, states);
						Integer totalItems =  service.getImportItemsCount(update, new HashSet<ItemState>());
						//loop through each item object to count error
						duration = Utils.dateDifference(update.getLocalDateStarted(), update.getLocalDateStopped(), TimeUnit.MINUTES).intValue();String status;

						if (!StringUtils.isBlank(update.getErrorMessage())){
							status = update.getErrorMessage();
						} else if( errors > 0) {
							status = errors + " errors";
						}
						else {
							status = totalItems + " items updated";
						}
						summaryList.add(new UpdateSummary(update.getImportId(), Utils.formatDateAuto(update.getLocalDateStarted()), duration, totalItems,status));
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
