package org.openmrs.module.openconceptlab.web.rest.resources;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportProgress;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.Utils;
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
import org.openmrs.module.webservices.rest.web.resource.api.Uploadable;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.GenericRestException;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Resource(name = RestConstants.VERSION_1  + OpenConceptLabRestController.OPEN_CONCEPT_LAB_REST_NAMESPACE + "/import", supportedClass = Import.class, supportedOpenmrsVersions = { "1.8.*",
        "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.0.*", "2.1.*", "2.2.*", "2.3.*", "2.4.*" })
public class ImportResource extends DelegatingCrudResource<Import> implements Uploadable {

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
        if (delegate != null) {
            UpdateScheduler updateScheduler = getUpdateScheduler();
            updateScheduler.scheduleNow();
            return getImportService().getLastImport();
        }
        throw new GenericRestException("Import cannot be null");
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
            description.addProperty("releaseVersion");
            description.addProperty("errorMessage");
            description.addProperty("importProgress");
            description.addProperty("importTime");
            description.addProperty("allItemsCount");
            description.addProperty("errorItemsCount");
            description.addProperty("ignoredErrorsCount");
            description.addProperty("updatedItemsCount");
            description.addProperty("upToDateItemsCount");
            description.addProperty("retiredItemsCount");
            description.addProperty("unretiredItemsCount");
            description.addProperty("addedItemsCount");
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
        return getImportService().getImportItemsCount(instance, states(ItemState.ERROR));
    }

    @PropertyGetter("ignoredErrorsCount")
    public static Integer getIgnoredErrorsCount(Import instance){
        return getImportService().getImportItemsCount(instance, states(ItemState.IGNORED_ERROR));
    }

    @PropertyGetter("updatedItemsCount")
    public static Integer getUpdatedItemsCount(Import instance){
        return getImportService().getImportItemsCount(instance, states(ItemState.UPDATED));
    }

    @PropertyGetter("upToDateItemsCount")
    public static Integer getUpToDateItemsCount(Import instance){
        return getImportService().getImportItemsCount(instance, states(ItemState.UP_TO_DATE));
    }

    @PropertyGetter("addedItemsCount")
    public static Integer getAddedItemsCount(Import instance){
        return getImportService().getImportItemsCount(instance, states(ItemState.ADDED));
    }

    @PropertyGetter("retiredItemsCount")
    public static Integer getRetiredItemsCount(Import instance){
        return getImportService().getImportItemsCount(instance, states(ItemState.RETIRED));
    }

    @PropertyGetter("unretiredItemsCount")
    public static Integer getUnretiredItemsCount(Import instance){
        return getImportService().getImportItemsCount(instance, states(ItemState.UNRETIRED));
    }

    private static Set<ItemState> states(ItemState... states) {
        Set<ItemState> set = new HashSet<ItemState>();
        set.addAll(Arrays.asList(states));
        return set;
    }

    @PropertyGetter("status")
    public static String getStatus(Import instance){
        String errorMessage = instance.getErrorMessage();
        if(StringUtils.isNotBlank(errorMessage)){
            return errorMessage;
        }

        Integer allItemsCount = getAllItemsCount(instance);
        Integer errors = getErrorItemsCount(instance);

        StringBuilder status = new StringBuilder();
        status.append(allItemsCount).append(" items fetched");
        if(errors > 0) {
            status.append(", \n").append(String.valueOf(errors) + " errors");
        }
        return status.toString();
    }

    private UpdateScheduler getUpdateScheduler() {
        return Context.getRegisteredComponent("openconceptlab.updateScheduler", UpdateScheduler.class);
    }

    private static ImportService getImportService() {
        return Context.getService(ImportService.class);
    }

    @Override
    public Object upload(MultipartFile multipartFile, RequestContext requestContext) throws ResponseException, IOException {
        if (multipartFile.isEmpty()) {
            throw new IllegalRequestException("File uploaded cannot be empty");
        }
        String originalFilename = multipartFile.getOriginalFilename();
        String fileBaseName = FilenameUtils.getBaseName(originalFilename);
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileExtension = FilenameUtils.getExtension(originalFilename);
        if (!fileExtension.equalsIgnoreCase("zip") && !fileExtension.equalsIgnoreCase("json")) {
            throw new IllegalRequestException("Supplied file must be a zip file or a json file");
        }
        String importFilename = fileBaseName + "_" + timestamp + "." + fileExtension;

        ImportService importService = getImportService();
        Importer importer = Context.getRegisteredComponent("openconceptlab.importer", Importer.class);

        File importFile = new File(Utils.getImportDirectory(), importFilename);
        multipartFile.transferTo(importFile);
        importer.setImportFile(importFile);

        String importType = requestContext.getParameter("importType");
        if ("concept".equals(importType)) {
            importer.importSingleConcept();
        }
        else {
            importer.importCollection();
        }
        return importService.getLastImport();
    }
}
