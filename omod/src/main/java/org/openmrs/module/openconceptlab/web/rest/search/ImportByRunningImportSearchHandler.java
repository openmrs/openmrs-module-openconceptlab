package org.openmrs.module.openconceptlab.web.rest.search;

import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.importer.Importer;
import org.openmrs.module.openconceptlab.web.rest.controller.OpenConceptLabRestController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.api.SearchConfig;
import org.openmrs.module.webservices.rest.web.resource.api.SearchHandler;
import org.openmrs.module.webservices.rest.web.resource.api.SearchQuery;
import org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;

@Component
public class ImportByRunningImportSearchHandler implements SearchHandler {

    @Autowired
    ImportService importService;

    @Autowired
    Importer importer;

    public void setImporter(Importer importer) {
        this.importer = importer;
    }

    private static final String RUNNING_IMPORT = "runningImport";

    private final SearchConfig searchConfig = new SearchConfig("default", RestConstants.VERSION_1 + OpenConceptLabRestController.OPEN_CONCEPT_LAB_REST_NAMESPACE + "/import",
            Arrays.asList("1.8.*", "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.0.*", "2.1.*", "2.2.*"),
            Arrays.asList(new SearchQuery.Builder("Allows you to get running import")
                    .withRequiredParameters(RUNNING_IMPORT)
                    .build()));

    @Override
    public SearchConfig getSearchConfig() {
        return this.searchConfig;
    }

    @Override
    public PageableResult search(RequestContext context) throws ResponseException {
        String runningImport = context.getParameter(RUNNING_IMPORT);

        if(Boolean.valueOf(runningImport) && importer.isRunning()){
            Import lastImport = importService.getLastImport();
            return new NeedsPaging<Import>(Collections.singletonList(lastImport), context);
        }

        return new EmptySearchResult();
    }
}
