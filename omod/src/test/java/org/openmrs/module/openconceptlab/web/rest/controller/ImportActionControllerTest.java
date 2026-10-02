package org.openmrs.module.openconceptlab.web.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
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

        Assertions.assertNotNull(newImportAction);
        List<Item> importItems = importService.getImportItems(importService.getImport(RestTestConstants.IMPORT_UUID), 0, 2, new HashSet<ItemState>());
        for(Item item: importItems){
            MatcherAssert.assertThat(item.getState(), is(not(ItemState.ERROR)));
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

        Assertions.assertNotNull(newImportAction);
        List<Item> importItems = importService.getImportItems(importService.getImport(RestTestConstants.IMPORT_UUID), 0, 2, new HashSet<ItemState>());
        for(Item item: importItems){
            MatcherAssert.assertThat(item.getState(), is(not(ItemState.IGNORED_ERROR)));
        }
    }

    @Override
    @Test
    public void shouldGetDefaultByUuid() throws Exception {
        Assertions.assertThrows(ResourceDoesNotSupportOperationException.class, () -> super.shouldGetDefaultByUuid());
    }

    @Override
    @Test
    public void shouldGetRefByUuid() throws Exception {
        Assertions.assertThrows(ResourceDoesNotSupportOperationException.class, () -> super.shouldGetRefByUuid());
    }

    @Override
    @Test
    public void shouldGetFullByUuid() throws Exception {
        Assertions.assertThrows(ResourceDoesNotSupportOperationException.class, () -> super.shouldGetFullByUuid());
    }

    @Override
    @Test
    public void shouldGetAll() throws Exception {
        Assertions.assertThrows(ResourceDoesNotSupportOperationException.class, () -> super.shouldGetAll());
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
