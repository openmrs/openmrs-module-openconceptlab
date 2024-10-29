package org.openmrs.module.openconceptlab.web.rest.resources;

import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.ItemType;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@SubResource(
        parent = ImportResource.class,
        path = "item",
        supportedClass = Item.class,
        supportedOpenmrsVersions = { "1.8.* - 2.*" }
)
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

    @Override
    public Schema<?> getGETSchema(Representation rep) {
        Schema<?> model = super.getGETSchema(rep);
        if (rep instanceof DefaultRepresentation) {
            model.addProperty("uuid", new UUIDSchema().example("uuid"));
            model.addProperty("type", new Schema<ItemType>()._enum(Arrays.asList(ItemType.values())));
            model.addProperty("url", new Schema<String>().format("uri"));
        } else if (rep instanceof FullRepresentation) {
            model.addProperty("uuid", new UUIDSchema().example("uuid"));
            model.addProperty("type", new Schema<ItemType>()._enum(Arrays.asList(ItemType.values())));
            model.addProperty("url", new Schema<String>().type("string").format("uri"));
            model.addProperty("type", new Schema<ItemState>()._enum(Arrays.asList(ItemState.values())));
            model.addProperty("hashedUrl", new Schema<String>().format("uri"));
            model.addProperty("versionUrl", new Schema<String>().format("uri"));
            model.addProperty("errorMessage", new StringSchema());
            model.addProperty("updatedOn", new DateTimeSchema());
        } else if (rep instanceof RefRepresentation) {
            model.addProperty("uuid", new UUIDSchema().example("uuid"));
            model.addProperty("type", new Schema<ItemType>()._enum(Arrays.asList(ItemType.values())));
        }
        return model;
    }

    private ImportService getImportService() {
        return Context.getService(ImportService.class);
    }
}
