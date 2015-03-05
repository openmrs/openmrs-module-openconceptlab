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

			message = "<b>The concept comes from Open Concept Lab subscription and any changes to the concept may be reverted with the next update</b>";

		}

		return message;
	}
}
