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
package org.openmrs.module.openconceptlab.fragment;

import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

/**
 * Fragment controller for un subscribing from ocl
 */
public class UnsubscribeFragmentController {

	public void controller(FragmentModel model,
							@SpringBean UpdateService service) {

		Subscription subscription = new Subscription();
		subscription.setUrl(null);
		subscription.setDays(null);
		subscription.setHours(null);
		subscription.setMinutes(null);

		//save the subscription
		service.saveSubscription(subscription);
	}
}
