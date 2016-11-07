package org.openmrs.module.openconceptlab.web.rest;

import java.util.UUID;

public class RestTestConstants {

    public static final String SUBSCRIPTION_UUID = UUID.nameUUIDFromBytes(RestTestConstants.SUBSCRIPTION_URL.getBytes()).toString();

    public static final String SUBSCRIPTION_TOKEN = "bd022mad6d3df24f5c42ewewa94b53a23edf6eee7r";

    public static final String SUBSCRIPTION_URL = "http://api.openconceptlab.com/orgs/CIEL/sources/CIEL/";

    public static final String ITEM_UUID = "ws353355-1691-11df-97a5-7038c432aabf";

    public static final String IMPORT_UUID = "dd553355-1691-11df-97a5-7038c432aabf";
}
