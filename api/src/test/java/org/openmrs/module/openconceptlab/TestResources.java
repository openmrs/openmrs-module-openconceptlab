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

import java.io.InputStream;


public class TestResources {
	
	public static InputStream getSimpleResponseAsStream() {
		return TestResources.class.getClassLoader().getResourceAsStream("response.zip");
	}
	
	public static InputStream getInitialResponseAsStream() {
		return TestResources.class.getClassLoader().getResourceAsStream("CIEL_20150514-testdata.20150622121229.tar");
	}
}
