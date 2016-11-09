package org.openmrs.module.openconceptlab.web.rest.resources;

import org.openmrs.module.openconceptlab.web.rest.controller.OpenConceptLabRestController;
import org.openmrs.module.openconceptlab.web.rest.helper.ImportAction;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * v. 2.0. If a copy of the MPL was not distributed with this file, You
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
@Resource(name = RestConstants.VERSION_1  + OpenConceptLabRestController.OPEN_CONCEPT_LAB_REST_NAMESPACE + "/importaction", supportedClass = ImportAction.class, supportedOpenmrsVersions = { "1.8.*",
        "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.0.*", "2.1.*" })
public class ImportActionResource extends BaseDelegatingResource<ImportAction> {
    @Override
    public ImportAction getByUniqueId(String s) {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    protected void delete(ImportAction importAction, String s, RequestContext requestContext) throws ResponseException {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public ImportAction newDelegate() {
        return new ImportAction();
    }

    @Override
    public ImportAction save(ImportAction importAction) {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public void purge(ImportAction importAction, RequestContext requestContext) throws ResponseException {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
        return null;
    }
}