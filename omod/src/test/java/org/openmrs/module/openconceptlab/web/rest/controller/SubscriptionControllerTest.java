package org.openmrs.module.openconceptlab.web.rest.controller;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.web.rest.RestTestConstants;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.test.Util;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceControllerTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

public class SubscriptionControllerTest extends MainResourceControllerTest{

    @Test
    public void shouldSaveNewSubscription() throws Exception {
        SimpleObject subscription = new SimpleObject();
        subscription.add("url", RestTestConstants.SUBSCRIPTION_URL);
        subscription.add("token", RestTestConstants.SUBSCRIPTION_TOKEN);

        String json = new ObjectMapper().writeValueAsString(subscription);

        MockHttpServletRequest req = request(RequestMethod.POST, getURI());
        req.setContent(json.getBytes());

        SimpleObject newSubscription = deserialize(handle(req));

        Assert.assertNotNull(newSubscription);
        Assert.assertEquals(getUuid(), newSubscription.get("uuid"));
    }

    @Test
    public void shouldDeleteSubscription() throws Exception {
        ImportService service = Context.getService(ImportService.class);
        service.saveSubscription(generateSubscription());

        Assert.assertNotNull(service.getSubscription());

        MockHttpServletRequest req = request(RequestMethod.DELETE, getURI() + "/" + getUuid());
        handle(req);

        Assert.assertNull(service.getSubscription());
    }

    @Test
    public void shouldGetSubscription() throws Exception {
        ImportService service = Context.getService(ImportService.class);
        service.saveSubscription(generateSubscription());

        MockHttpServletRequest req = request(RequestMethod.GET, getURI());

        SimpleObject result = deserialize(handle(req));
        List<Object> results = Util.getResultsList(result);
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
    }

    @Override
    public void shouldGetDefaultByUuid() throws Exception {
        Context.getService(ImportService.class).saveSubscription(generateSubscription());
        super.shouldGetDefaultByUuid();
    }

    @Override
    public void shouldGetRefByUuid() throws Exception {
        Context.getService(ImportService.class).saveSubscription(generateSubscription());
        super.shouldGetRefByUuid();
    }

    @Override
    public void shouldGetFullByUuid() throws Exception {
        Context.getService(ImportService.class).saveSubscription(generateSubscription());
        super.shouldGetFullByUuid();
    }

    @Override
    public String getURI() {
        return "openconceptlab/subscription";
    }

    @Override
    public String getUuid() {
        return RestTestConstants.SUBSCRIPTION_UUID;
    }


    @Override
    public long getAllCount() {
        return 1;
    }

    private Subscription generateSubscription(){
        Subscription sub = new Subscription();
        sub.setToken(RestTestConstants.SUBSCRIPTION_TOKEN);
        sub.setUrl(RestTestConstants.SUBSCRIPTION_URL);
        sub.setUuid(RestTestConstants.SUBSCRIPTION_UUID);
        return sub;
    }
}
