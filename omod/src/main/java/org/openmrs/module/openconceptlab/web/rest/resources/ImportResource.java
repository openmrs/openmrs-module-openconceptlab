package org.openmrs.module.openconceptlab.web.rest.resources;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportProgress;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.importer.Importer;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;
import org.openmrs.module.openconceptlab.web.rest.controller.OpenConceptLabRestController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.HashSet;
import java.util.Set;

@Resource(name = RestConstants.VERSION_1  + OpenConceptLabRestController.OPEN_CONCEPT_LAB_REST_NAMESPACE + "/import", supportedClass = Import.class, supportedOpenmrsVersions = { "1.8.*",
        "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.0.*", "2.1.*" })
public class ImportResource extends DelegatingCrudResource<Import> {

    @Override
    public Import getByUniqueId(String uniqueId) {
        return getImportService().getImport(uniqueId);
    }

    @Override
    protected void delete(Import delegate, String reason, RequestContext context) throws ResponseException {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public Import newDelegate() {
        return new Import();
    }

    @Override
    public Import save(Import delegate) {
        Import anImport = getImportService().getLastImport();
        if (anImport != null) {
            getImportService().ignoreAllErrors(anImport);
        }
        UpdateScheduler updateScheduler = getUpdateScheduler();
        updateScheduler.scheduleNow();
        return getImportService().getLastImport();
    }

    @Override
    public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
        return new DelegatingResourceDescription();
    }

    @Override
    public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public void purge(Import delegate, RequestContext context) throws ResponseException {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        if (rep instanceof FullRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("localDateStarted");
            description.addProperty("localDateStopped");
            description.addProperty("oclDateStarted");
            description.addProperty("errorMessage");
            description.addProperty("importProgress");
            description.addProperty("importTime");
            description.addProperty("allItemsCount");
            description.addProperty("errorItemsCount");
            description.addProperty("ignoredErrorsCount");
            description.addProperty("status");
            description.addLink("ref", ".?v=" + RestConstants.REPRESENTATION_REF);
            description.addSelfLink();
            return description;
        } else if (rep instanceof DefaultRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("localDateStarted");
            description.addProperty("localDateStopped");
            description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
            description.addLink("ref", ".?v=" + RestConstants.REPRESENTATION_REF);
            description.addSelfLink();
            return description;
        } else if (rep instanceof RefRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addSelfLink();
            return description;
        }
        return null;
    }

    @Override
    protected PageableResult doGetAll(RequestContext context) throws ResponseException {
        return new NeedsPaging<Import>(getImportService().getImportsInOrder(0, 20), context);
    }

    @PropertyGetter("importProgress")
    public static String getImportProgress(Import instance){
        Importer importer = Context.getRegisteredComponent("openconceptlab.importer", Importer.class);
        ImportProgress importProgress = importer.getImportProgress(instance.getUuid());
        return String.valueOf(importProgress.getProgress());
    }

    @PropertyGetter("importTime")
    public static String getImportTime(Import instance){
        Importer importer = Context.getRegisteredComponent("openconceptlab.importer", Importer.class);
        ImportProgress importProgress = importer.getImportProgress(instance.getUuid());
        return importProgress.getTimeText();
    }

    @PropertyGetter("allItemsCount")
    public static Integer getAllItemsCount(Import instance){
        return getImportService().getImportItemsCount(instance, new HashSet<ItemState>());
    }

    @PropertyGetter("errorItemsCount")
    public static Integer getErrorItemsCount(Import instance){
        Set<ItemState> inError = new HashSet<ItemState>();
        inError.add(ItemState.ERROR);
        return getImportService().getImportItemsCount(instance, inError);
    }

    @PropertyGetter("ignoredErrorsCount")
    public static Integer getIgnoredErrorsCount(Import instance){
        Set<ItemState> ignoredError = new HashSet<ItemState>();
        ignoredError.add(ItemState.IGNORED_ERROR);
        return getImportService().getImportItemsCount(instance, ignoredError);
    }

    @PropertyGetter("status")
    public static String getStatus(Import instance){
        Set<ItemState> states = new HashSet<ItemState>();
        states.add(ItemState.ERROR);
        Integer errors = getImportService().getImportItemsCount(instance, states);
        Integer totalItems =  getImportService().getImportItemsCount(instance, new HashSet<ItemState>());
        String errorMessage = instance.getErrorMessage();
        if(StringUtils.isNotBlank(errorMessage)){
            return errorMessage;
        } else if(errors > 0){
            return String.valueOf(errors)+ " errors";
        } else {
            return String.valueOf(totalItems) + " items updated";
        }
    }

    private UpdateScheduler getUpdateScheduler() {
        return Context.getRegisteredComponent("openconceptlab.updateScheduler", UpdateScheduler.class);
    }

    private static ImportService getImportService() {
        return Context.getService(ImportService.class);
    }
}
