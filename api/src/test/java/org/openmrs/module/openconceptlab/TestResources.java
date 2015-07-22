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
