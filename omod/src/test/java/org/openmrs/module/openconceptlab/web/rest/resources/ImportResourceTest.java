package org.openmrs.module.openconceptlab.web.rest.resources;

import org.junit.Before;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.web.rest.RestTestConstants;
import org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResourceTest;

public class ImportResourceTest extends BaseDelegatingResourceTest<ImportResource, Import> {

    @Before
    public void setUp() throws Exception {
        executeDataSet("test_dataset.xml");
    }

    @Override
    public Import newObject() {
        return Context.getService(ImportService.class).getImport(getUuidProperty());
    }

    @Override
    public String getDisplayProperty() {
        return "import";
    }

    @Override
    public String getUuidProperty() {
        return RestTestConstants.IMPORT_UUID;
    }

    @Override
    public void validateRefRepresentation() throws Exception {
        assertPropEquals("uuid", getObject().getUuid());
        assertPropPresent("links");
    }

    @Override
    public void validateDefaultRepresentation() throws Exception {
        assertPropEquals("uuid", getObject().getUuid());
        assertPropEquals("localDateStarted", getObject().getLocalDateStarted());
        assertPropEquals("localDateStopped", getObject().getLocalDateStopped());
        assertPropPresent("links");
    }

    @Override
    public void validateFullRepresentation() throws Exception {
        assertPropEquals("uuid", getObject().getUuid());
        assertPropEquals("localDateStarted", getObject().getLocalDateStarted());
        assertPropEquals("localDateStopped", getObject().getLocalDateStopped());
        assertPropEquals("oclDateStarted", getObject().getOclDateStarted());
        assertPropEquals("errorMessage", getObject().getErrorMessage());
        assertPropPresent("importProgress");
        assertPropPresent("importTime");
        assertPropPresent("allItemsCount");
        assertPropPresent("errorItemsCount");
        assertPropPresent("ignoredErrorsCount");
        assertPropPresent("status");
        assertPropPresent("links");
    }
}
