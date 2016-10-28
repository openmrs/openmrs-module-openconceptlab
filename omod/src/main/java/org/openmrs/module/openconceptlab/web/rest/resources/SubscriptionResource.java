package org.openmrs.module.openconceptlab.web.rest.resources;

import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.Utils;
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

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

@Resource(name = RestConstants.VERSION_1 + OpenConceptLabRestController.OPEN_CONCEPT_LAB_REST_NAMESPACE + "/subscription", supportedClass = Subscription.class, supportedOpenmrsVersions = { "1.8.*",
        "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.0.*", "2.1.*" })
public class SubscriptionResource extends DelegatingCrudResource<Subscription> {

    @Override
    public Subscription getByUniqueId(String uniqueId) {
        return getImportService().getSubscription();
    }

    @Override
    protected void delete(Subscription delegate, String reason, RequestContext context) throws ResponseException {
        getImportService().unsubscribe();
    }

    @Override
    public Subscription newDelegate() {
        return new Subscription();
    }

    @Override
    public Subscription save(Subscription delegate) {
        UpdateScheduler updateScheduler = getUpdateScheduler();
        updateScheduler.schedule(delegate);
        return getImportService().getSubscription();
    }

    @Override
    public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
        DelegatingResourceDescription delegatingResourceDescription = new DelegatingResourceDescription();
        delegatingResourceDescription.addRequiredProperty("url");
        delegatingResourceDescription.addRequiredProperty("token");
        delegatingResourceDescription.addProperty("days");
        delegatingResourceDescription.addProperty("hours");
        delegatingResourceDescription.addProperty("minutes");
        return delegatingResourceDescription;
    }

    @Override
    public void purge(Subscription delegate, RequestContext context) throws ResponseException {
        getImportService().unsubscribe();
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        if (rep instanceof FullRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("url");
            description.addProperty("token");
            description.addProperty("days");
            description.addProperty("hours");
            description.addProperty("minutes");
            description.addProperty("nextUpdate");
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

    @PropertyGetter("uuid")
    public static String getUuid(Subscription instance){
        return UUID.nameUUIDFromBytes(instance.getToken().getBytes()).toString();
    }

    @PropertyGetter("nextUpdate")
    public static String getNextUpdate(Subscription instance){
        ImportService service = getImportService();
        if (instance != null) {
            if(instance.isManual()){
                return "manuall";
            } else {
                Date lastDate = service.getLastImport().getLocalDateStarted();
                Integer days = instance.getDays();
                Integer hours = instance.getHours();
                Integer minutes = instance.getMinutes();
                return Utils.formatedDate(Utils.dateAddDays(lastDate, days)) + " " + appendZeros(hours.toString()) + ":" + appendZeros(minutes.toString());
            }
        }
        return null;
    }

    private static String appendZeros(String k) {
        String results = k;
        if (k.length() < 2) {
            results = "0" + k;
        }
        return results;
    }

    private UpdateScheduler getUpdateScheduler() {
        return Context.getRegisteredComponent("openconceptlab.updateScheduler", UpdateScheduler.class);
    }

    private static ImportService getImportService() {
        return Context.getService(ImportService.class);
    }
}
