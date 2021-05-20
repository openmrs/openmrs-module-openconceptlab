/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.client;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
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
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.module.openconceptlab.MockTest;
import org.openmrs.module.openconceptlab.TestResources;
import org.openmrs.module.openconceptlab.client.OclClient.OclResponse;

public class OclClientTest extends MockTest {

	@Mock
	GetMethod get;

	OclClient oclClient;

	File tempDir;

	private static final String URL = "https://api.openconceptlab.org/users/username/collections/collectionname/";

	private static final String URL_WITH_VERSION = "https://api.openconceptlab.org/users/username/collections/collectionname/v1.0";

	private static final String URL_WITHOUT_VERSION = "https://api.openconceptlab.org/users/username/collections/collectionname";

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
		when(get.getResponseHeader("Date")).thenReturn(header);
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
		assertThat(json, containsString("\"extras\": {\"about\": \"Source managed by Andrew Kanter\"}"));
		assertThat(json.length(), is(266223));
	}
	
	@Test
	public void extractResponse_shouldExtractDateAndGzippedJson() throws Exception {
		String date = "Wed, 22 Jul 2015 13:07:19 GMT";
		Header header = new Header("Date", date);
		
		when(get.getPath()).thenReturn("https://ocl-source-export-staging.s3.amazonaws.com/CIEL/CIEL_20150514-testdata.20150622121229.tgz"
				+ "?Signature=k%2FG0J%2Bt%2BlYJoscWxNFYbn%2BvtiPo%3D&Expires=1437567014&AWSAccessKeyId=AKIAJSVYSQTANHNWOOPQ");
		when(get.getResponseHeader("Date")).thenReturn(header);
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

	@Test
	public void getOclReleaseVersion_shouldGetOclReleaseVersion() throws IOException {
		String subscriptionToken = "53fc72f0498a707a26e4d903c0f24c2db24d1e35";
		String version = oclClient.getOclReleaseVersion(URL_WITH_VERSION, subscriptionToken);
		assertThat(version, is("v1.0"));
	}

	@Test
	public void getExportUrl_shouldConstructCorrectExportUrlWhenVersionIsPassedToUrl() throws URIException {
		String collectionVersion = "v1.0";
		String getMethodUrlWithVersion = oclClient.getExportUrl(URL_WITH_VERSION, collectionVersion);
		assertThat(getMethodUrlWithVersion, is(URL_WITH_VERSION + "/export"));
	}

	@Test
	public void getExportUrl_shouldConstructCorrectExportUrlWhenVersionIsNotPassedToUrl() throws URIException {
		String collectionVersion = "v1.0";
		String getMethodUrlWithoutVersion = oclClient.getExportUrl(URL_WITHOUT_VERSION, collectionVersion);
		assertThat(getMethodUrlWithoutVersion, is(URL_WITHOUT_VERSION + "/v1.0" + "/export"));
	}

	@Test
	public void removeLastUrlForwardSlashIfExist_shouldRemoveLastForwardSlashIfItExist() {
		String newUrl = oclClient.removeLastUrlForwardSlashIfExist(URL);
		int numberOfForwardSlashesInUrl = StringUtils.countMatches(URL, "/");

		int numberOfForwardSlashesInNewUrl = StringUtils.countMatches(newUrl, "/");

		assertThat(numberOfForwardSlashesInUrl, equalTo(numberOfForwardSlashesInNewUrl + 1));
	}
}
