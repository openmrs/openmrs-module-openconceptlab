/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.extension.html;

import org.openmrs.Concept;
import org.openmrs.api.context.Context;
import org.openmrs.module.Extension;
import org.openmrs.module.openconceptlab.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by codehub on 04/03/15.
 */
@Component
public class HighlightSubscribedConcept extends Extension {

	volatile static UpdateService service;

	@Autowired
	public void setService(UpdateService service) {
		HighlightSubscribedConcept.service = service;
	}

	@Override
	public MEDIA_TYPE getMediaType() {
		return Extension.MEDIA_TYPE.html;
	}

	public String getOverrideContent(String bodyContent) {

		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

		Concept concept = Context.getConceptService().getConcept(Integer.parseInt(request.getParameter("conceptId")));

		String message = "";

		if(service.isSubscribedConcept(concept.getUuid())) {

			message = "<div class=\"highlight\">This concept was downloaded from your Open Concept Lab subscription. Any local changes that you make to this concept here will be lost the next time you download updates from the OCL server. \n\n We strongly recommend that you do not edit this concept locally.</div>";

		}
		
		return message;
	}
}
