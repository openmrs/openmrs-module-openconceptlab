package org.openmrs.module.openconceptlab.client;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.module.openconceptlab.MockTest;
import org.openmrs.module.openconceptlab.TestResources;
import org.openmrs.module.openconceptlab.client.OclClient;
import org.openmrs.module.openconceptlab.client.OclClient.OclResponse;

public class OclClientTest extends MockTest {
	
	@Mock
	GetMethod get;
	
	OclClient oclClient;
	
	File tempDir;
	
	@Before
	public void createTempDir() throws IOException {
		tempDir = File.createTempFile("ocl", "");
		FileUtils.deleteQuietly(tempDir);
		tempDir.mkdir();
		tempDir.deleteOnExit();
		
		oclClient = new OclClient(tempDir.getAbsolutePath());
	}
	
	@After
	public void deleteTempDir() throws IOException {
		FileUtils.deleteQuietly(tempDir);
	}
	
	/**
	 * @see OclClient#extractResponse(GetMethod)
	 * @verifies extract date and json
	 */
	@Test
	public void extractResponse_shouldExtractDateAndJson() throws Exception {
		String date = "Mon, 13 Oct 2014 11:07:19 GMT";
		Header header = new Header("Date", date);
		
		when(get.getResponseHeader("date")).thenReturn(header);
		when(get.getResponseBodyAsStream()).thenReturn(TestResources.getSimpleResponseAsStream());
		
		OclResponse subscription = oclClient.extractResponse(get);
		InputStream in = subscription.getContentStream();
		String json = "";
		try {
			json = IOUtils.toString(in, "utf-8");
			in.close();
		} finally {
			IOUtils.closeQuietly(in);
		}
		
		assertThat(subscription.getUpdatedTo(), is(DateUtil.parseDate(date)));
		assertThat(json, startsWith("{\"type\": \"Source\", \"uuid\": \"54e74b378a86f251d2e737d8\""));
		assertThat(json.length(), is(266183));
	}
}
