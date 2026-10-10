package org.openmrs.module.openconceptlab.web.rest.search;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.importer.Importer;
import org.openmrs.module.openconceptlab.web.rest.RestTestConstants;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.test.Util;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceControllerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

import static org.mockito.Mockito.doReturn;

public class ImportByRunningImportSearchHandlerTest extends MainResourceControllerTest{

    private ImportService service;

    @Autowired
    @InjectMocks
    ImportByRunningImportSearchHandler importByRunningImportSearchHandler;

    @Mock
    Importer importer;

    @BeforeEach
    public void setUp() throws Exception {
        executeDataSet("test_dataset.xml");
        service = Context.getService(ImportService.class);
    }

    @Test
    public void shouldGetRunningImport() throws Exception {
        doReturn(true).when(importer).isRunning();
        MockHttpServletRequest req = request(RequestMethod.GET, getURI());
        req.addParameter("runningImport", "true");
        SimpleObject result = deserialize(handle(req));
        List<Object> results = Util.getResultsList(result);
        Assertions.assertNotNull(results);
        Assertions.assertEquals(1, results.size());
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
