package org.openmrs.module.openconceptlab.web.rest.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
    public void setUp() throws Exception {
        executeDataSet("test_dataset.xml");
        service = Context.getService(ImportService.class);
    }

    @Test
    public void shouldGetTwoImports() throws Exception {
        MockHttpServletRequest req = request(RequestMethod.GET, getURI());

        SimpleObject result = deserialize(handle(req));
        List<Object> results = Util.getResultsList(result);
        Assertions.assertNotNull(results);
        Assertions.assertEquals(2, results.size());
    }

    @Test
    public void shouldGetImportInFullRep() throws Exception {
        MockHttpServletRequest req = request(RequestMethod.GET, getURI() + "/" + getUuid());
        req.addParameter("v", "full");
        SimpleObject result = deserialize(handle(req));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(getUuid(), result.get("uuid"));
        Assertions.assertEquals("100", result.get("importProgress"));
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
