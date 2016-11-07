package org.openmrs.module.openconceptlab.web.rest.search;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.web.rest.RestTestConstants;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.test.Util;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceControllerTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashSet;
import java.util.List;

public class ItemsByStateSearchHandlerTest extends MainResourceControllerTest{

    private ImportService service;

    @Before
    public void setUp() throws Exception {
        executeDataSet("test_dataset.xml");
        service = Context.getService(ImportService.class);
    }

    @Test
    public void shouldGetAllUpdatedItems() throws Exception {
        MockHttpServletRequest req = request(RequestMethod.GET, getURI());
        req.addParameter("state", "ERROR");
        SimpleObject result = deserialize(handle(req));
        List<Object> results = Util.getResultsList(result);
        Assert.assertNotNull(results);
        Assert.assertEquals(getUpdatedItemsCount(), results.size());
    }

    @Override
    public String getURI() {
        return "openconceptlab/import/" + RestTestConstants.IMPORT_UUID +"/item";
    }

    @Override
    public String getUuid() {
        return RestTestConstants.ITEM_UUID;
    }

    @Override
    public long getAllCount() {
        return service.getImportItemsCount(getImport(), new HashSet<ItemState>());
    }

    private Import getImport(){
        return service.getImport(RestTestConstants.IMPORT_UUID);
    }

    private int getUpdatedItemsCount(){
        HashSet<ItemState> states = new HashSet<ItemState>();
        states.add(ItemState.UPDATED);
        return service.getImportItemsCount(getImport(), states);
    }
}
