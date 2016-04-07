/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab;

import org.openmrs.module.DaemonToken;
import org.openmrs.module.ModuleFactory;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;


public class TestResources {
	
	public static InputStream getSimpleResponseAsStream() {
		return TestResources.class.getClassLoader().getResourceAsStream("response.zip");
	}

	public static InputStream getEmptyResponseAsStream() {
		return TestResources.class.getClassLoader().getResourceAsStream("emptyResponse.zip");
	}
	
	public static InputStream getInitialResponseAsStream() {
		return TestResources.class.getClassLoader().getResourceAsStream("CIEL_20150514-testdata.20150622121229.tar");
	}
	
	@SuppressWarnings("unchecked")
    public static void setupDaemonToken() {
		Map<String, DaemonToken> daemonTokens;
	    try {
	    	Field field = ModuleFactory.class.getDeclaredField("daemonTokens");
	    	field.setAccessible(true);
	    	daemonTokens = (Map<String, DaemonToken>) field.get(null);
	    } catch (Exception e) {
	    	throw new RuntimeException(e);
	    }
		
		DaemonToken daemonToken = new DaemonToken("openconceptlab");
		daemonTokens.put(daemonToken.getId(), daemonToken);
		new OpenConceptLabActivator().setDaemonToken(daemonToken);
    }
}
