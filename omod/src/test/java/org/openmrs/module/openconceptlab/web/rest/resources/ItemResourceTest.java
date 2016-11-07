package org.openmrs.module.openconceptlab.web.rest.resources;

import org.junit.Before;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.web.rest.RestTestConstants;
import org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResourceTest;

public class ItemResourceTest extends BaseDelegatingResourceTest<ItemResource, Item> {

    @Before
    public void setUp() throws Exception {
        executeDataSet("test_dataset.xml");
    }

    @Override
    public Item newObject() {
        return Context.getService(ImportService.class).getItem(getUuidProperty());
    }

    @Override
    public String getDisplayProperty() {
        return "item";
    }

    @Override
    public String getUuidProperty() {
        return RestTestConstants.ITEM_UUID;
    }

    @Override
    public void validateRefRepresentation() throws Exception {
        assertPropEquals("uuid", getObject().getUuid());
        assertPropPresent("type");
        assertPropPresent("links");
    }

    @Override
    public void validateDefaultRepresentation() throws Exception {
        assertPropEquals("uuid", getObject().getUuid());
        assertPropPresent("type");
        assertPropPresent("url");
        assertPropPresent("links");
    }

    @Override
    public void validateFullRepresentation() throws Exception {
        assertPropEquals("uuid", getObject().getUuid());
        assertPropPresent("type");
        assertPropPresent("url");
        assertPropPresent("state");
        assertPropPresent("hashedUrl");
        assertPropPresent("versionUrl");
        assertPropPresent("errorMessage");
        assertPropPresent("updatedOn");
        assertPropPresent("links");
    }
}
