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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.openconceptlab.client.OclClient;


public class TestResources {
	
	public static InputStream getSimpleResponseAsStream() {
		return TestResources.class.getClassLoader().getResourceAsStream("response.zip");
	}

	public static InputStream getResponseAsStream(String testResourcePath) {
		return TestResources.class.getClassLoader().getResourceAsStream(testResourcePath);
	}

	public static URL getSimpleResponseZipFileUrl() {
		return TestResources.class.getClassLoader().getResource("response.zip");
	}

	public static File getSimpleZipFile() throws IOException, URISyntaxException {
		File zipFile = new File(getSimpleResponseZipFileUrl().toURI());
		File newZipFile = new File(getSimpleResponseZipFileUrl().getPath() + "new_file.zip");
		IOUtils.copy(new FileInputStream(zipFile), new FileOutputStream(newZipFile));
		return newZipFile;
	}

	public static InputStream getSimpleResponseAsJsonStream() {
		InputStream inputStream = getSimpleResponseAsStream();
		OclClient oclClient = new OclClient();
		try {
			OclClient.OclResponse oclResponse = oclClient.unzipResponse(inputStream, null);
			return oclResponse.getContentStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
