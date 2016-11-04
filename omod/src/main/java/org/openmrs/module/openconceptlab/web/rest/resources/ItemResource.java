package org.openmrs.module.openconceptlab.web.rest.resources;

import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.SubResource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingSubResource;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.HashSet;
import java.util.List;

@SubResource(parent = ImportResource.class, path = "item", supportedClass = Item.class, supportedOpenmrsVersions = { "1.8.*",
        "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.0.*", "2.1.*" })
public class ItemResource extends DelegatingSubResource<Item, Import, ImportResource>{

    @Override
    public Import getParent(Item instance) {
        return instance.getAnImport();
    }

    @Override
    public void setParent(Item instance, Import parent) {
        instance.setAnImport(parent);
    }

    @Override
    public PageableResult doGetAll(Import parent, RequestContext context) throws ResponseException {
        ImportService importService = getImportService();
        Integer updateItemsCount = importService.getImportItemsCount(parent, new HashSet<ItemState>());
        List<Item> updateItems = importService.getImportItems(parent, 0, updateItemsCount, new HashSet<ItemState>());
        return new NeedsPaging<Item>(updateItems, context);
    }

    @Override
    public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public Item getByUniqueId(String uuid) {
        return getImportService().getItem(uuid);
    }

    @Override
    protected void delete(Item delegate, String reason, RequestContext context) throws ResponseException {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public Item newDelegate() {
        return new Item();
    }

    @Override
    public Item save(Item delegate) {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public void purge(Item delegate, RequestContext context) throws ResponseException {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        if (rep instanceof DefaultRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("type");
            description.addProperty("url");
            description.addLink("ref", ".?v=" + RestConstants.REPRESENTATION_REF);
            description.addSelfLink();
            return description;
        } else if (rep instanceof FullRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("type");
            description.addProperty("url");
            description.addProperty("state");
            description.addProperty("hashedUrl");
            description.addProperty("versionUrl");
            description.addProperty("errorMessage");
            description.addProperty("updatedOn");
            description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
            description.addLink("ref", ".?v=" + RestConstants.REPRESENTATION_REF);
            description.addSelfLink();
            return description;
        } else if (rep instanceof RefRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("type");
            description.addSelfLink();
            return description;
        }
        return null;
    }

    private ImportService getImportService() {
        return Context.getService(ImportService.class);
    }
}
