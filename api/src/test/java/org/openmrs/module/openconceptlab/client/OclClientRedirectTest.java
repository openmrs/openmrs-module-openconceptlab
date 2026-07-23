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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Exercises {@link OclClient}'s HTTP handling against real, local HTTP servers so that the
 * redirect-following behaviour can be verified end to end. OCL's export endpoints answer with a
 * cross-host redirect (production returns 302, older deployments 303) to a pre-signed S3 URL —
 * simulated here by a second server on another port — and may first answer with a relative
 * trailing-slash 301 (Django's APPEND_SLASH). These tests confirm the subscription token reaches
 * the OCL origin (including same-origin redirect hops, needed for private collections) but is never
 * forwarded to a cross-origin redirect target (which would make S3 reject the request), and that
 * relative {@code Location} values are resolved rather than executed as host-less requests.
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

	/** A second server on a different port, standing in for the cross-origin S3 download host. */
	private HttpServer s3Server;

	private String s3BaseUrl;

	private File tempDir;

	private OclClient oclClient;

	/** The {@code Authorization} header observed by the server for each requested path ("" if absent). */
	private final Map<String, String> authHeaderByPath = new ConcurrentHashMap<String, String>();

	/** The raw query string observed by the server for each requested path ("" if absent). */
	private final Map<String, String> queryByPath = new ConcurrentHashMap<String, String>();

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

		s3Server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		s3Server.start();
		s3BaseUrl = "http://127.0.0.1:" + s3Server.getAddress().getPort();
	}

	@After
	public void tearDown() {
		if (server != null) {
			server.stop(0);
		}
		if (s3Server != null) {
			s3Server.stop(0);
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
		server.createContext("/coll/v1.0/export", redirectTo(FOUND, s3BaseUrl + "/download"));
		s3Server.createContext("/download", respondWith(200, "application/zip", "export-data"));

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
	public void executeExportRequest_shouldStripTokenAcrossEveryCrossOriginRedirectHop() throws IOException {
		server.createContext("/coll/v1.0/export", redirectTo(FOUND, s3BaseUrl + "/hop1"));
		s3Server.createContext("/hop1", redirectTo(FOUND, s3BaseUrl + "/hop2"));
		s3Server.createContext("/hop2", respondWith(200, "application/zip", "export-data"));

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

	/**
	 * Models the production failure: OCL (Django) answers the slash-less export URL with a relative
	 * trailing-slash 301, which used to be executed as a host-less request ("host parameter is
	 * null"). The relative location must be resolved against the request URL, and since that hop
	 * stays on the OCL origin, the token must be kept — private collections reject the request
	 * otherwise. The subsequent cross-origin hop to S3 must still go out token-free.
	 */
	@Test
	public void executeExportRequest_shouldResolveRelativeRedirectAndKeepTokenOnSameOriginHop() throws IOException {
		// The JDK server matches contexts by prefix, so this handler sees both the slash-less
		// request and the trailing-slash one, mirroring Django's APPEND_SLASH behaviour.
		server.createContext("/coll/v1.0/export", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				if (exchange.getRequestURI().getPath().endsWith("/")) {
					redirectTo(FOUND, s3BaseUrl + "/download").handle(exchange);
				} else {
					redirectTo(MOVED_PERMANENTLY, "/coll/v1.0/export/").handle(exchange);
				}
			}
		});
		s3Server.createContext("/download", respondWith(200, "application/zip", "export-data"));

		GetMethod result = oclClient.executeExportRequest(baseUrl + "/coll", "v1.0", TOKEN);
		try {
			assertThat(result.getStatusCode(), is(200));
			assertThat(authHeaderByPath.get("/coll/v1.0/export"), is(AUTHORIZATION));
			assertThat("token must be kept on the same-origin redirect hop",
					authHeaderByPath.get("/coll/v1.0/export/"), is(AUTHORIZATION));
			assertThat("token must not be forwarded to the cross-origin redirect target",
					authHeaderByPath.get("/download"), is(""));
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

	/**
	 * fetchSnapshotUpdates sends its parameters (includeMappings, updatedSince, limit, ...) as a
	 * query string. Django's APPEND_SLASH 301 carries the query string in its relative
	 * {@code Location}, and RFC-compliant resolution takes the query from the location, not the
	 * base — so the parameters must survive the redirected hop, along with the token (same origin).
	 */
	@Test
	public void fetchSnapshotUpdates_shouldKeepQueryParametersAndTokenAcrossRelativeTrailingSlashRedirect()
			throws IOException {
		server.createContext("/coll", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				if (exchange.getRequestURI().getPath().endsWith("/")) {
					respondWithBytes(200, "application/zip", zipWithExportJson()).handle(exchange);
				} else {
					String query = exchange.getRequestURI().getRawQuery();
					redirectTo(MOVED_PERMANENTLY, "/coll/" + (query == null ? "" : "?" + query)).handle(exchange);
				}
			}
		});

		OclClient.OclResponse response = oclClient.fetchSnapshotUpdates(baseUrl + "/coll", TOKEN, new Date(0));

		assertThat(response.getContentStream().read(), is((int) '{'));
		assertThat(queryByPath.get("/coll"), containsString("limit=100000"));
		assertThat(queryByPath.get("/coll"), containsString("updatedSince="));
		assertThat("query parameters must survive the redirected hop", queryByPath.get("/coll/"),
				is(queryByPath.get("/coll")));
		assertThat(authHeaderByPath.get("/coll/"), is(AUTHORIZATION));
	}

	@Test
	public void executeExportRequest_shouldReportMalformedLocationHeaderAsDiagnosableIOException() throws IOException {
		String malformedLocation = "http://[bad";
		server.createContext("/coll/v1.0/export", redirectTo(FOUND, malformedLocation));

		try {
			GetMethod result = oclClient.executeExportRequest(baseUrl + "/coll", "v1.0", TOKEN);
			result.releaseConnection();
			fail("Expected an IOException for the malformed Location header");
		}
		catch (IOException expected) {
			assertThat(expected.getMessage(), containsString(malformedLocation));
			assertThat(expected.getMessage(), containsString("/coll/v1.0/export"));
		}
	}

	@Test
	public void executeExportRequest_shouldRejectRedirectThatIsMissingALocationHeader() throws IOException {
		server.createContext("/coll/v1.0/export", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				recordAuth(exchange);
				exchange.sendResponseHeaders(FOUND, -1);
				exchange.close();
			}
		});

		try {
			GetMethod result = oclClient.executeExportRequest(baseUrl + "/coll", "v1.0", TOKEN);
			result.releaseConnection();
			fail("Expected an IOException for a redirect without a Location header");
		}
		catch (IOException expected) {
			// Redirect following stops and executeExportRequest rejects the still-3xx response.
		}
	}

	/**
	 * A protocol-relative location ({@code //host/path}) inherits the scheme from the request being
	 * redirected but changes the host — CDN/S3 fronts emit these. It must resolve to an absolute
	 * URL and count as cross-origin for the token decision.
	 */
	@Test
	public void executeExportRequest_shouldResolveProtocolRelativeLocationAndTreatItAsCrossOrigin() throws IOException {
		String protocolRelativeLocation = s3BaseUrl.substring("http:".length()) + "/download";
		server.createContext("/coll/v1.0/export", redirectTo(FOUND, protocolRelativeLocation));
		s3Server.createContext("/download", respondWith(200, "application/zip", "export-data"));

		GetMethod result = oclClient.executeExportRequest(baseUrl + "/coll", "v1.0", TOKEN);
		try {
			assertThat(result.getStatusCode(), is(200));
			assertThat("token must not be forwarded to the cross-origin redirect target",
					authHeaderByPath.get("/download"), is(""));
		}
		finally {
			result.releaseConnection();
		}
	}

	/**
	 * The origin check compares each hop against the ORIGINAL request, not the previous hop, so a
	 * cross-origin detour that redirects back to the OCL origin gets the token re-attached. This
	 * pins that deliberate choice.
	 */
	@Test
	public void executeExportRequest_shouldReattachTokenWhenCrossOriginHopRedirectsBackToOriginalOrigin()
			throws IOException {
		server.createContext("/coll/v1.0/export", redirectTo(FOUND, s3BaseUrl + "/hop"));
		s3Server.createContext("/hop", redirectTo(FOUND, baseUrl + "/final"));
		server.createContext("/final", respondWith(200, "application/zip", "export-data"));

		GetMethod result = oclClient.executeExportRequest(baseUrl + "/coll", "v1.0", TOKEN);
		try {
			assertThat(result.getStatusCode(), is(200));
			assertThat(authHeaderByPath.get("/hop"), is(""));
			assertThat(authHeaderByPath.get("/final"), is(AUTHORIZATION));
		}
		finally {
			result.releaseConnection();
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
		server.createContext("/coll/v1.0/export", redirectTo(redirectStatus, s3BaseUrl + "/download"));
		s3Server.createContext("/download", respondWith(200, "application/zip", "export-data"));

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
		server.createContext("/coll/v1.0/export", redirectTo(redirectStatus, s3BaseUrl + "/download"));

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
		String query = exchange.getRequestURI().getRawQuery();
		queryByPath.put(exchange.getRequestURI().getPath(), query == null ? "" : query);
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
		return respondWithBytes(status, contentType, body.getBytes(StandardCharsets.UTF_8));
	}

	private HttpHandler respondWithBytes(final int status, final String contentType, final byte[] bytes) {
		return new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				recordAuth(exchange);
				exchange.getResponseHeaders().add("Content-Type", contentType);
				exchange.sendResponseHeaders(status, bytes.length);
				try (OutputStream out = exchange.getResponseBody()) {
					out.write(bytes);
				}
			}
		};
	}

	/** A minimal archive of the shape OCL exports use, so response extraction can complete. */
	private byte[] zipWithExportJson() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(bytes);
		zip.putNextEntry(new ZipEntry("export.json"));
		zip.write("{}".getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
		zip.close();
		return bytes.toByteArray();
	}
}