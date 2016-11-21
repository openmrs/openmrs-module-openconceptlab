package org.openmrs.module.openconceptlab.web.rest.resources;

import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;
import org.openmrs.module.openconceptlab.web.rest.controller.OpenConceptLabRestController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.Collections;

@Resource(name = RestConstants.VERSION_1 + OpenConceptLabRestController.OPEN_CONCEPT_LAB_REST_NAMESPACE + "/subscription", supportedClass = Subscription.class, supportedOpenmrsVersions = { "1.8.*",
        "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.0.*", "2.1.*" })
public class SubscriptionResource extends DelegatingCrudResource<Subscription> {

    @Override
    public Subscription getByUniqueId(String uniqueId) {
        Subscription subscription = getImportService().getSubscription();
        if(subscription.getUuid().equals(uniqueId)){
            return subscription;
        } else {
            throw new ObjectNotFoundException();
        }
    }

    @Override
    protected void delete(Subscription subscription, String reason, RequestContext context) throws ResponseException {
        getImportService().unsubscribe();
    }

    @Override
    public Subscription newDelegate() {
        return new Subscription();
    }

    @Override
    public Subscription save(Subscription subscription) {
        if (!"url".equals(subscription.getUrl())) {
            UpdateScheduler updateScheduler = getUpdateScheduler();
            updateScheduler.schedule(subscription);
        }
        return getImportService().getSubscription();
    }

    @Override
    public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
        DelegatingResourceDescription delegatingResourceDescription = new DelegatingResourceDescription();
        delegatingResourceDescription.addRequiredProperty("url");
        delegatingResourceDescription.addRequiredProperty("token");
        delegatingResourceDescription.addProperty("subscribedToSnapshot");
        delegatingResourceDescription.addProperty("validationType");
        return delegatingResourceDescription;
    }

    @Override
    public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
        DelegatingResourceDescription delegatingResourceDescription = new DelegatingResourceDescription();
        delegatingResourceDescription.addProperty("url");
        delegatingResourceDescription.addProperty("token");
        delegatingResourceDescription.addProperty("subscribedToSnapshot");
        delegatingResourceDescription.addProperty("validationType");
        return delegatingResourceDescription;
    }

    @Override
    public void purge(Subscription delegate, RequestContext context) throws ResponseException {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        if (rep instanceof FullRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("url");
            description.addProperty("token");
            description.addProperty("subscribedToSnapshot");
            description.addProperty("validationType");
            description.addLink("ref", ".?v=" + RestConstants.REPRESENTATION_REF);
            description.addSelfLink();
            return description;
        } else if (rep instanceof DefaultRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("url");
            description.addProperty("token");
            description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
            description.addLink("ref", ".?v=" + RestConstants.REPRESENTATION_REF);
            description.addSelfLink();
            return description;
        } else if (rep instanceof RefRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("url");
            description.addSelfLink();
            return description;
        }
        return null;
    }

    @Override
    protected PageableResult doGetAll(RequestContext context) throws ResponseException {
        return new NeedsPaging<Subscription>(Collections.singletonList(getImportService().getSubscription()), context);
    }

    @PropertyGetter("subscribedToSnapshot")
    public boolean getSubscribedToSnapshot(Subscription subscription){
        return subscription.isSubscribedToSnapshot();
    }

    @PropertySetter("subscribedToSnapshot")
    public void setSubscribedToSnapshot(Subscription subscription, Object value){
        subscription.setSubscribedToSnapshot(Boolean.valueOf(value.toString()));
    }

    private UpdateScheduler getUpdateScheduler() {
        return Context.getRegisteredComponent("openconceptlab.updateScheduler", UpdateScheduler.class);
    }

    private static ImportService getImportService() {
        return Context.getService(ImportService.class);
    }
}
