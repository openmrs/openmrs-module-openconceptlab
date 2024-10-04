/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * v. 2.0. If a copy of the MPL was not distributed with this file, You
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.web.rest.resources;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.RefProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.web.rest.controller.OpenConceptLabRestController;
import org.openmrs.module.openconceptlab.web.rest.helper.ImportAction;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

@Resource(
        name = RestConstants.VERSION_1 + OpenConceptLabRestController.OPEN_CONCEPT_LAB_REST_NAMESPACE + "/importaction",
        supportedClass = ImportAction.class,
        supportedOpenmrsVersions = { "1.8.* - 2.*" }
)
public class ImportActionResource extends DelegatingCrudResource<ImportAction> {
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
        Import anImport = importAction.getAnImport();
        ImportService importService = getImportService();
        if (importAction.isIgnoreAllErrors()) {
            importService.ignoreAllErrors(anImport);
        }
        return importAction;
    }

    @Override
    public void purge(ImportAction importAction, RequestContext requestContext) throws ResponseException {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
        DelegatingResourceDescription delegatingResourceDescription = new DelegatingResourceDescription();
        delegatingResourceDescription.addRequiredProperty("anImport");
        delegatingResourceDescription.addRequiredProperty("ignoreAllErrors");
        return delegatingResourceDescription;
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
        DelegatingResourceDescription delegatingResourceDescription = new DelegatingResourceDescription();
        delegatingResourceDescription.addProperty("anImport", Representation.DEFAULT);
        delegatingResourceDescription.addProperty("ignoreAllErrors");
        return delegatingResourceDescription;
    }

    @Override
    public Model getGETModel(Representation rep) {
        ModelImpl model = (ModelImpl) super.getGETModel(rep);
        model.property("anImport", new RefProperty("#/definitions/OpenConceptLabImportGet")).property("ignoreAllErrors", new BooleanProperty());
        return model;
    }

    @Override
    public Model getCREATEModel(Representation rep) {
        return new ModelImpl().property("anImport", new RefProperty("#/definitions/OpenConceptLabImportCreate")).property("ignoreAllErrors", new BooleanProperty());
    }

    @PropertySetter("ignoreAllErrors")
    public ImportAction setIgnoreAllErrors(ImportAction importAction, Object value){
        importAction.setIgnoreAllErrors(Boolean.parseBoolean(value.toString()));
        return importAction;
    }

    @PropertyGetter("ignoreAllErrors")
    public boolean isIgnoreAllErrors(ImportAction importAction){
        return importAction.isIgnoreAllErrors();
    }

    private ImportService getImportService() {
        return Context.getService(ImportService.class);
    }

}
