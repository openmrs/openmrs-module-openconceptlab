package org.openmrs.module.openconceptlab.web.rest.controller;

import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rest/" + RestConstants.VERSION_1 + OpenConceptLabRestController.OPEN_CONCEPT_LAB_REST_NAMESPACE)
public class OpenConceptLabRestController extends MainResourceController{

    public static final String OPEN_CONCEPT_LAB_REST_NAMESPACE = "/openconceptlab";

    @Override
    public String getNamespace() {
        return RestConstants.VERSION_1 + OPEN_CONCEPT_LAB_REST_NAMESPACE;
    }
}
