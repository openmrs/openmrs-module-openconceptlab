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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Exercises {@link OclClient}'s HTTP handling against a real, local HTTP server so that the
 * redirect-following behaviour can be verified end to end. OCL's export endpoints answer with a
 * cross-host redirect (production returns 302, older deployments 303) to a pre-signed S3 URL; these
 * tests confirm the subscription token reaches the OCL host but is never forwarded to the redirect
 * target (which would make S3 reject the request), regardless of which 3xx code is used.
 */
public class OclClientRedirectTest {

	private static final String TOKEN = "53fc72f0498a707a26e4d903c0f24c2db24d1e35";

	private static final String AUTHORIZATION = "Token " + TOKEN;

	private static final int MOVED_PERMANENTLY = 301;

	private static final int FOUND = 302;

	private static final int SEE_OTHER = 303;

	private static final int TEMPORARY_REDIRECT = 307;

	private HttpServer server;

	private String baseUrl;

	private File tempDir;

	private OclClient oclClient;

	/** The {@code Authorization} header observed by the server for each requested path ("" if absent). */
	private final Map<String, String> authHeaderByPath = new ConcurrentHashMap<String, String>();

	@Before
	public void setUp() throws IOException {
		tempDir = File.createTempFile("ocl", "");
		FileUtils.deleteQuietly(tempDir);
		tempDir.mkdir();
		tempDir.deleteOnExit();

		oclClient = new OclClient(tempDir.getAbsolutePath());

		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@After
	public void tearDown() {
		if (server != null) {
			server.stop(0);
		}
		FileUtils.deleteQuietly(tempDir);
	}

	@Test
	public void executeExportRequest_shouldFollowMovedPermanently301WithoutForwardingToken() throws IOException {
		assertExportFollowsRedirectWithoutForwardingToken(MOVED_PERMANENTLY);
	}

	@Test
	public void executeExportRequest_shouldFollowFound302WithoutForwardingToken() throws IOException {
		assertExportFollowsRedirectWithoutForwardingToken(FOUND);
	}

	@Test
	public void executeExportRequest_shouldFollowSeeOther303WithoutForwardingToken() throws IOException {
		assertExportFollowsRedirectWithoutForwardingToken(SEE_OTHER);
	}

	@Test
	public void executeExportRequest_shouldFollowTemporaryRedirect307WithoutForwardingToken() throws IOException {
		assertExportFollowsRedirectWithoutForwardingToken(TEMPORARY_REDIRECT);
	}

	@Test
	public void executeExportRequest_shouldNotSendAuthorizationHeaderWhenNoTokenIsConfigured() throws IOException {
		server.createContext("/coll/v1.0/export", redirectTo(FOUND, baseUrl + "/download"));
		server.createContext("/download", respondWith(200, "application/zip", "export-data"));

		GetMethod result = oclClient.executeExportRequest(baseUrl + "/coll", "v1.0", null);
		try {
			assertThat(result.getStatusCode(), is(200));
			assertThat(authHeaderByPath.get("/coll/v1.0/export"), is(""));
			assertThat(authHeaderByPath.get("/download"), is(""));
		}
		finally {
			result.releaseConnection();
		}
	}

	@Test
	public void executeExportRequest_shouldStripTokenAcrossEveryRedirectHop() throws IOException {
		server.createContext("/coll/v1.0/export", redirectTo(FOUND, baseUrl + "/hop1"));
		server.createContext("/hop1", redirectTo(FOUND, baseUrl + "/hop2"));
		server.createContext("/hop2", respondWith(200, "application/zip", "export-data"));

		GetMethod result = oclClient.executeExportRequest(baseUrl + "/coll", "v1.0", TOKEN);
		try {
			assertThat(result.getStatusCode(), is(200));
			assertThat(authHeaderByPath.get("/coll/v1.0/export"), is(AUTHORIZATION));
			assertThat(authHeaderByPath.get("/hop1"), is(""));
			assertThat(authHeaderByPath.get("/hop2"), is(""));
		}
		finally {
			result.releaseConnection();
		}
	}

	@Test
	public void executeExportRequest_shouldStopFollowingRedirectsAtTheConfiguredLimit() throws IOException {
		// A redirect pointing back at itself would loop forever if the hop count were not bounded.
		server.createContext("/coll/v1.0/export", redirectTo(FOUND, baseUrl + "/loop"));
		server.createContext("/loop", redirectTo(FOUND, baseUrl + "/loop"));

		try {
			GetMethod result = oclClient.executeExportRequest(baseUrl + "/coll", "v1.0", TOKEN);
			result.releaseConnection();
			fail("Expected an IOException once the redirect limit was exceeded");
		}
		catch (IOException expected) {
			// Following stops at the limit and leaves a still-redirecting response, which
			// executeExportRequest rejects because its status is not 200.
		}
	}

	@Test
	public void fetchLatestOclReleaseVersion_shouldTreatFound302ExportAsAnExistingVersion() throws IOException {
		assertVersionProbeTreatsRedirectAsExistingVersion(FOUND);
	}

	@Test
	public void fetchLatestOclReleaseVersion_shouldTreatSeeOther303ExportAsAnExistingVersion() throws IOException {
		assertVersionProbeTreatsRedirectAsExistingVersion(SEE_OTHER);
	}

	private void assertExportFollowsRedirectWithoutForwardingToken(int redirectStatus) throws IOException {
		server.createContext("/coll/v1.0/export", redirectTo(redirectStatus, baseUrl + "/download"));
		server.createContext("/download", respondWith(200, "application/zip", "export-data"));

		GetMethod result = oclClient.executeExportRequest(baseUrl + "/coll", "v1.0", TOKEN);
		try {
			assertThat(result.getStatusCode(), is(200));
			assertThat(authHeaderByPath.get("/coll/v1.0/export"), is(AUTHORIZATION));
			assertThat("token must not be forwarded to the redirect target", authHeaderByPath.get("/download"), is(""));
		}
		finally {
			result.releaseConnection();
		}
	}

	private void assertVersionProbeTreatsRedirectAsExistingVersion(int redirectStatus) throws IOException {
		server.createContext("/coll/versions", respondWith(200, "application/json", "[{\"id\":\"HEAD\"},{\"id\":\"v1.0\"}]"));
		server.createContext("/coll/v1.0/export", redirectTo(redirectStatus, baseUrl + "/download"));

		String version = oclClient.fetchLatestOclReleaseVersion(baseUrl + "/coll", TOKEN);

		assertThat(version, is("v1.0"));
		assertThat(authHeaderByPath.get("/coll/versions"), is(AUTHORIZATION));
		assertThat(authHeaderByPath.get("/coll/v1.0/export"), is(AUTHORIZATION));
		// The probe checks for the redirect itself and must not chase it to the download target.
		assertThat(authHeaderByPath.containsKey("/download"), is(false));
	}

	private void recordAuth(HttpExchange exchange) {
		String auth = exchange.getRequestHeaders().getFirst("Authorization");
		authHeaderByPath.put(exchange.getRequestURI().getPath(), auth == null ? "" : auth);
	}

	private HttpHandler redirectTo(final int status, final String location) {
		return new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				recordAuth(exchange);
				exchange.getResponseHeaders().add("Location", location);
				exchange.sendResponseHeaders(status, -1);
				exchange.close();
			}
		};
	}

	private HttpHandler respondWith(final int status, final String contentType, final String body) {
		return new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				recordAuth(exchange);
				byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", contentType);
				exchange.sendResponseHeaders(status, bytes.length);
				try (OutputStream out = exchange.getResponseBody()) {
					out.write(bytes);
				}
			}
		};
	}
}