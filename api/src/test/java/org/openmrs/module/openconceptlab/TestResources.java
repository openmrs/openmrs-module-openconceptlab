package org.openmrs.module.openconceptlab;

import java.io.InputStream;


public class TestResources {
	
	public static InputStream getSimpleResponseAsStream() {
		return TestResources.class.getClassLoader().getResourceAsStream("response.zip");
	}
}
