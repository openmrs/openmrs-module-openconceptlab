package org.openmrs.module.openconceptlab.web.rest.resources;


import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.web.rest.RestTestConstants;
import org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResourceTest;

public class SubscriptionResourceTest extends BaseDelegatingResourceTest<SubscriptionResource, Subscription> {

    @Override
    public Subscription newObject() {
        Subscription subscription = new Subscription();
        subscription.setUuid(getUuidProperty());
        subscription.setToken(RestTestConstants.SUBSCRIPTION_TOKEN);
        subscription.setUrl(RestTestConstants.SUBSCRIPTION_URL);
        subscription.setSubscribedToSnapshot(false);
        return subscription;
    }

    @Override
    public void validateRefRepresentation() throws Exception {
        assertPropEquals("uuid", getObject().getUuid());
        assertPropEquals("url", getObject().getUrl());
        assertPropPresent("links");
    }

    @Override
    public void validateDefaultRepresentation() throws Exception {
        assertPropEquals("uuid", getObject().getUuid());
        assertPropEquals("url", getObject().getUrl());
        assertPropEquals("token", getObject().getToken());
        assertPropPresent("links");
        assertPropPresent("resourceVersion");
    }

    @Override
    public void validateFullRepresentation() throws Exception {
        assertPropEquals("uuid", getObject().getUuid());
        assertPropEquals("url", getObject().getUrl());
        assertPropEquals("token", getObject().getToken());
        assertPropPresent("subscribedToSnapshot");
        assertPropPresent("links");
        assertPropPresent("resourceVersion");
    }

    @Override
    public String getDisplayProperty() {
        return "subscription";
    }

    @Override
    public String getUuidProperty() {
        return RestTestConstants.SUBSCRIPTION_UUID;
    }

}
