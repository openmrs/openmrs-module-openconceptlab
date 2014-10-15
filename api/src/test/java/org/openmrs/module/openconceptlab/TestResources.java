package org.openmrs.module.openconceptlab;

import java.io.InputStream;


public class TestResources {
	
	public static InputStream getResponseAsStream() {
		return TestResources.class.getClassLoader().getResourceAsStream("response.zip");
	}
}
