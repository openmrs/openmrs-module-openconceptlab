package org.openmrs.module.openconceptlab.web.rest.controller;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.web.rest.RestTestConstants;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceControllerTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class ImportActionControllerTest extends MainResourceControllerTest {

    private ImportService importService;

    @Before
    public void setUp() throws Exception {
        executeDataSet("test_dataset.xml");
        importService = Context.getService(ImportService.class);
    }

    @Test
    public void shouldIgnoreAllErrors() throws Exception {
        SimpleObject importAction = new SimpleObject();
        importAction.add("ignoreAllErrors", "true");
        importAction.add("anImport", RestTestConstants.IMPORT_UUID);

        String json = new ObjectMapper().writeValueAsString(importAction);

        MockHttpServletRequest req = request(RequestMethod.POST, getURI());
        req.setContent(json.getBytes());

        SimpleObject newImportAction = deserialize(handle(req));

        Assert.assertNotNull(newImportAction);
        List<Item> importItems = importService.getImportItems(importService.getImport(RestTestConstants.IMPORT_UUID), 0, 2, new HashSet<ItemState>());
        for(Item item: importItems){
            Assert.assertThat(item.getState(), is(not(ItemState.ERROR)));
        }
    }

    @Test
    public void shouldNotIgnoreAllErrors() throws Exception {
        SimpleObject importAction = new SimpleObject();
        importAction.add("ignoreAllErrors", "false");
        importAction.add("anImport", RestTestConstants.IMPORT_UUID);

        String json = new ObjectMapper().writeValueAsString(importAction);

        MockHttpServletRequest req = request(RequestMethod.POST, getURI());
        req.setContent(json.getBytes());

        SimpleObject newImportAction = deserialize(handle(req));

        Assert.assertNotNull(newImportAction);
        List<Item> importItems = importService.getImportItems(importService.getImport(RestTestConstants.IMPORT_UUID), 0, 2, new HashSet<ItemState>());
        for(Item item: importItems){
            Assert.assertThat(item.getState(), is(not(ItemState.IGNORED_ERROR)));
        }
    }

    @Override
    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void shouldGetDefaultByUuid() throws Exception {
        super.shouldGetDefaultByUuid();
    }

    @Override
    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void shouldGetRefByUuid() throws Exception {
        super.shouldGetRefByUuid();
    }

    @Override
    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void shouldGetFullByUuid() throws Exception {
        super.shouldGetFullByUuid();
    }

    @Override
    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void shouldGetAll() throws Exception {
        super.shouldGetAll();
    }

    @Override
    public String getURI() {
        return "openconceptlab/importaction";
    }

    @Override
    public String getUuid() {
        return null;
    }

    @Override
    public long getAllCount() {
        return 1;
    }
}
