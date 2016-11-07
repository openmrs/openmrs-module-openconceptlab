package org.openmrs.module.openconceptlab.web.rest.search;

import org.apache.commons.lang.StringUtils;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.web.rest.controller.OpenConceptLabRestController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.api.SearchConfig;
import org.openmrs.module.webservices.rest.web.resource.api.SearchQuery;
import org.openmrs.module.webservices.rest.web.resource.api.SubResourceSearchHandler;
import org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

@Component
public class ItemsByStateSearchHandler implements SubResourceSearchHandler {

    @Autowired
    ImportService importService;

    private static final String ITEM_STATE = "state";

    private final SearchConfig searchConfig = new SearchConfig("default", RestConstants.VERSION_1 + OpenConceptLabRestController.OPEN_CONCEPT_LAB_REST_NAMESPACE + "/import/item",
            Arrays.asList("1.8.*", "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.0.*", "2.1.*"),
            Arrays.asList(new SearchQuery.Builder("Allows you to get items by state")
                    .withRequiredParameters(ITEM_STATE)
                    .build()));

    @Override
    public SearchConfig getSearchConfig() {
        return this.searchConfig;
    }

    @Override
    public PageableResult search(RequestContext requestContext) throws ResponseException {
        throw new UnsupportedOperationException("Cannot search for item without parent import");
    }

    @Override
    public PageableResult search(String parentUuid, RequestContext requestContext) throws ResponseException {
        String itemState = requestContext.getParameter(ITEM_STATE);
        if(StringUtils.isBlank(itemState) || StringUtils.isBlank(parentUuid)){
            return new EmptySearchResult();
        }
        List<Item> itemList = new LinkedList<Item>();
        Import anImport = importService.getImport(parentUuid);
        Integer importItemsCount = importService.getImportItemsCount(anImport, new HashSet<ItemState>());
        List<Item> importItems = importService.getImportItems(anImport, 0, importItemsCount, new HashSet<ItemState>());
        for(Item item : importItems){
            if(itemState.equals(item.getState().name())){
                itemList.add(item);
            }
        }
        if(!itemList.isEmpty()){
            return new NeedsPaging<Item>(itemList, requestContext);
        } else {
            return new EmptySearchResult();
        }
    }
}
