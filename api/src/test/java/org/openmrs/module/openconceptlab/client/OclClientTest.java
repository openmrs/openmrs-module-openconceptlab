package org.openmrs.module.openconceptlab.client;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

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
	public void extractResponse_shouldExtractDateAndZippedJson() throws Exception {
		String date = "Mon, 13 Oct 2014 11:07:19 GMT";
		Header header = new Header("Date", date);
		
		when(get.getResponseHeader("Content-Type")).thenReturn(new Header("Content-Type", "application/zip"));
		when(get.getResponseHeader("date")).thenReturn(header);
		when(get.getResponseBodyAsStream()).thenReturn(TestResources.getSimpleResponseAsStream());
		
		OclResponse subscription = oclClient.extractResponse(get);
		InputStream in = subscription.getContentStream();
		String json = "";
		try {
			json = IOUtils.toString(in, "utf-8");
			in.close();
		}
		finally {
			IOUtils.closeQuietly(in);
		}
		
		assertThat(subscription.getUpdatedTo(), is(DateUtil.parseDate(date)));
		assertThat(json, startsWith("{\"type\": \"Source\", \"uuid\": \"54e74b378a86f251d2e737d8\""));
		assertThat(json.length(), is(266183));
	}
	
	@Test
	public void extractResponse_shouldExtractDateAndGzippedJson() throws Exception {
		String date = "Wed, 22 Jul 2015 13:07:19 GMT";
		Header header = new Header("Date", date);
		
		when(get.getPath()).thenReturn("https://ocl-source-export-staging.s3.amazonaws.com/CIEL/CIEL_20150514-testdata.20150622121229.tgz"
				+ "?Signature=k%2FG0J%2Bt%2BlYJoscWxNFYbn%2BvtiPo%3D&Expires=1437567014&AWSAccessKeyId=AKIAJSVYSQTANHNWOOPQ");
		when(get.getResponseHeader("date")).thenReturn(header);
		when(get.getResponseBodyAsStream()).thenReturn(TestResources.getInitialResponseAsStream());
		
		OclResponse subscription = oclClient.extractResponse(get);
		InputStream in = subscription.getContentStream();
		String json = "";
		try {
			json = IOUtils.toString(in, "utf-8");
			in.close();
		}
		finally {
			IOUtils.closeQuietly(in);
		}
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(0); //reset
		calendar.set(2015, 5, 22, 12, 12, 29);
		
		assertThat(subscription.getUpdatedTo(), is(calendar.getTime()));
		assertThat(json.substring(0, 512), startsWith("{\"type\": \"Source\", \"uuid\": \"5582be2550d61b5538ed694b\""));
		assertThat(json.length(), is(7540529));
	}
	
	@Test
	public void parseDateFromPath_shouldParseDate() throws Exception {
		String path = "https://ocl-source-export-staging.s3.amazonaws.com/CIEL/CIEL_20150514-testdata.20150622121229.tgz"
				+ "?Signature=k%2FG0J%2Bt%2BlYJoscWxNFYbn%2BvtiPo%3D&Expires=1437567014&AWSAccessKeyId=AKIAJSVYSQTANHNWOOPQ";
		
		Date date = oclClient.parseDateFromPath(path);
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(0); //reset
		calendar.set(2015, 5, 22, 12, 12, 29);
		
		assertThat(date, is(calendar.getTime()));
	}
}
