package org.openmrs.module.openconceptlab.web.rest.controller;

import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainSubResourceController;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.openmrs.module.openconceptlab.web.rest.controller.OpenConceptLabRestController.OPEN_CONCEPT_LAB_REST_NAMESPACE;

@Component
@RequestMapping("/rest/" + RestConstants.VERSION_1 + OPEN_CONCEPT_LAB_REST_NAMESPACE)
public class OpenConceptLabRestSubController  extends MainSubResourceController{

    @Override
    public String getNamespace() {
        return RestConstants.VERSION_1 + OPEN_CONCEPT_LAB_REST_NAMESPACE;
    }
}
