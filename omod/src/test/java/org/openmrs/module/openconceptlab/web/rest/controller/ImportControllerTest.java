package org.openmrs.module.openconceptlab.web.rest.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.web.rest.RestTestConstants;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.test.Util;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceControllerTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

public class ImportControllerTest extends MainResourceControllerTest{

    private ImportService service;

    @Before
    public void setUp() throws Exception {
        executeDataSet("test_dataset.xml");
        service = Context.getService(ImportService.class);
    }

    @Test
    public void shouldGetTwoImports() throws Exception {
        MockHttpServletRequest req = request(RequestMethod.GET, getURI());

        SimpleObject result = deserialize(handle(req));
        List<Object> results = Util.getResultsList(result);
        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
    }

    @Test
    public void shouldGetImportInFullRep() throws Exception {
        MockHttpServletRequest req = request(RequestMethod.GET, getURI() + "/" + getUuid());
        req.addParameter("v", "full");
        SimpleObject result = deserialize(handle(req));

        Assert.assertNotNull(result);
        Assert.assertEquals(getUuid(), result.get("uuid"));
        Assert.assertEquals("100", result.get("importProgress"));
    }

    @Override
    public String getURI() {
        return "openconceptlab/import";
    }

    @Override
    public String getUuid() {
        return RestTestConstants.IMPORT_UUID;
    }

    @Override
    public long getAllCount() {
        return service.getImportsInOrder(0, 100).size();
    }
}
