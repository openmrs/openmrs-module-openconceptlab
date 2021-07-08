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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.util.OpenmrsUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class OclClient {

	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	public static final String FILE_NAME_FORMAT = "yyyyMMdd_HHmmss";

	private static final Pattern ARCHIVE_DATE_PATTERN = Pattern.compile("(?<date>[0-9]*).(?<extension>tgz|zip)");

	private static final int BUFFER_SIZE = 64 * 1024;

	private static final int NUMBER_OF_SLASHES_AFTER_BASE_URL = 5;

	private final static int TIMEOUT_IN_MS = 128000;

	private final String dataDirectory;

	private long bytesDownloaded = 0;

	private long totalBytesToDownload = 0;

	private final HttpClient client = new HttpClient();

	{
		AuthPolicy.registerAuthScheme("token", OclTokenAuthenticationScheme.class);
		HttpClientParams params = client.getParams();
		params.setSoTimeout(TIMEOUT_IN_MS);
		params.setAuthenticationPreemptive(true);
	}

	public OclClient() {
		dataDirectory = OpenmrsUtil.getApplicationDataDirectory();
	}

	public OclClient(String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}

	public OclResponse fetchSnapshotUpdates(String url, String token, Date updatedSince) throws IOException {
		totalBytesToDownload = -1; //unknown yet
		bytesDownloaded = 0;

		GetMethod get = constructGetMethod(url, token);

		get.addRequestHeader("Compress", "true");

		List<NameValuePair> query = new ArrayList<NameValuePair>();
		query.add(new NameValuePair("includeMappings", "true"));
		query.add(new NameValuePair("includeConcepts", "true"));
		query.add(new NameValuePair("includeRetired", "true"));
		query.add(new NameValuePair("limit", "100000"));

		if (updatedSince != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			query.add(new NameValuePair("updatedSince", dateFormat.format(updatedSince)));
		}

		get.setQueryString(query.toArray(new NameValuePair[0]));

		try {
			client.executeMethod(get);
		}
		finally {
			client.getState().clearCredentials();
		}

		if (get.getStatusCode() != 200) {
			throw new IOException(get.getStatusLine().toString());
		}

		return extractResponse(get);
	}

	public OclResponse fetchOclConcepts(String url, String token) throws IOException {
		totalBytesToDownload = -1; //unknown yet
		bytesDownloaded = 0;

		String collectionVersion = getOclReleaseVersion(url, token);

		GetMethod exportUrlGet = executeExportRequest(url, collectionVersion, token);

		return extractResponse(exportUrlGet);
	}

	public GetMethod executeExportRequest(String url, String collectionVersion, String token) throws IOException {
		GetMethod exportUrlGet = executeGetMethod(getExportUrl(url, collectionVersion), token);

		if (exportUrlGet.getStatusCode() != 200) {
			throw new IOException(exportUrlGet.getStatusLine().toString());
		}

		return exportUrlGet;
	}

	/**
	 * This constructs the url to import concepts from the subscription url provided
	 *
	 * @param url     the subscription url
	 * @param version desired version of the specified collection
	 * @return The export URL
	 */
	public String getExportUrl(String url, String version) {
		url = removeLastUrlForwardSlashIfExist(url);

		if (url.contains(version)) {
			return url + "/export";
		} else {
			return url + "/" + version + "/export";
		}
	}

	/**
	 * This gets the desired collection release version
	 *
	 * @param url,   the subscription url
	 * @param token, the subscription API token
	 * @return the retrieved collection version
	 * @throws IOException if the URL cannot be parsed
	 */
	public String getOclReleaseVersion(String url, String token) throws IOException {
		String exportVersion;
		url = removeLastUrlForwardSlashIfExist(url);
		URL subscriptionURL = new URL(url);

		String subUrlAfterBaseUrl = subscriptionURL.getPath();

		int count = StringUtils.countMatches(subUrlAfterBaseUrl, "/");
		/*
		 *This checks if collection version has been passed to subscription url by checking number of forward slashes after ocl base url
		 *If the number is 5, such as with https://api.openconceptlab.org/users/username/collections/collectionname/v1.0
		 *that means collection version was passed and it's assigned to exportVersion
		 */
		if (count == NUMBER_OF_SLASHES_AFTER_BASE_URL) {
			exportVersion = url.substring(url.lastIndexOf("/") + 1);
		} else {
			exportVersion = fetchLatestOclReleaseVersion(url, token);
		}

		return exportVersion;
	}

	/**
	 * This changes the url by removing the last forward slash if it exists
	 *
	 * @param url, this is the subscription url
	 * @return the url format expected by the ocl server
	 */
	public String removeLastUrlForwardSlashIfExist(String url) {
		if (url.endsWith("/")) {
			url = url.substring(0, url.lastIndexOf('/'));
		}
		return url;
	}

	public OclResponse fetchOclConcepts(String url, String token, String lastReleaseVersion) throws IOException {
		String versionToImport = getOclReleaseVersion(url, token);
		if (lastReleaseVersion == null || !lastReleaseVersion.equals(versionToImport)) {
			//If there is no lastReleaseVersion then the subscription has been changed from snapshot to releases
			//and we need to fetch the latest OCL release version.
			//If lastReleaseVersion does not match the latest OCL release version then we need to fetch it too.
			return fetchOclConcepts(url, token);
		} else {
			//No new version
			return null;
		}
	}

	OclResponse extractResponse(GetMethod get) throws IOException {
		Header dateHeader = get.getResponseHeader("Date");
		Date date;
		try {
			SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
			date = format.parse(dateHeader.getValue());
		}
		catch (ParseException e) {
			throw new IOException("Cannot parse date header", e);
		}

		File file = newFile(date);
		download(get.getResponseBodyAsStream(), get.getResponseContentLength(), file);

		InputStream response = new FileInputStream(file);
		Header contentTypeHeader = get.getResponseHeader("Content-Type");
		if (contentTypeHeader != null && "application/zip".equals(contentTypeHeader.getValue())) {
			return unzipResponse(response, date);
		} else {
			date = parseDateFromPath(get.getPath());

			return ungzipAndUntarResponse(response, date);
		}
	}

	Date parseDateFromPath(String path) throws IOException {
		Matcher matcher = ARCHIVE_DATE_PATTERN.matcher(path);
		if (matcher.find()) {
			String foundDate = matcher.group("date");
			try {
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
				return format.parse(foundDate);
			}
			catch (ParseException e) {
				throw new IOException("Cannot parse date from path " + path, e);
			}
		}
		throw new IOException("Cannot find date in path " + path);
	}

	@SuppressWarnings("resource")
	public static OclResponse ungzipAndUntarResponse(InputStream response, Date date) throws IOException {
		GZIPInputStream gzipIn = new GZIPInputStream(response);
		TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn);
		boolean foundEntry = false;
		try {
			TarArchiveEntry entry = tarIn.getNextTarEntry();
			while (entry != null) {
				if (entry.getName().equals("export.json")) {
					foundEntry = true;
					return new OclResponse(tarIn, entry.getSize(), date);
				}
				entry = tarIn.getNextTarEntry();
			}

			tarIn.close();
		}
		finally {
			if (!foundEntry) {
				IOUtils.closeQuietly(tarIn);
			}
		}
		throw new IOException("Unsupported format of response. Expected tar.gz archive with export.json.");
	}

	@SuppressWarnings("resource")
	public static OclResponse unzipResponse(InputStream response, Date date) throws IOException {
		ZipInputStream zip = new ZipInputStream(response);
		boolean foundEntry = false;
		try {
			ZipEntry entry = zip.getNextEntry();
			while (entry != null) {
				if (entry.getName().equals("export.json")) {
					foundEntry = true;
					return new OclResponse(zip, entry.getSize(), date);
				}
				entry = zip.getNextEntry();
			}

			zip.close();
		}
		finally {
			if (!foundEntry) {
				IOUtils.closeQuietly(zip);
			}
		}
		throw new IOException("Unsupported format of response. Expected zip with export.json.");
	}

	public File newFile(Date date) {
		SimpleDateFormat fileNameFormat = new SimpleDateFormat(FILE_NAME_FORMAT);
		String fileName = fileNameFormat.format(date) + ".zip";
		File oclDirectory = new File(dataDirectory, "ocl");
		if (!oclDirectory.exists()) {
			oclDirectory.mkdirs();
		}
		return new File(oclDirectory, fileName);
	}

	void download(InputStream in, long length, File destination) throws IOException {
		OutputStream out = null;
		try {
			totalBytesToDownload = length;
			bytesDownloaded = 0;

			out = new BufferedOutputStream(new FileOutputStream(destination), BUFFER_SIZE);

			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = in.read(buffer, 0, buffer.length)) != -1) {
				out.write(buffer, 0, bytesRead);
				bytesDownloaded += bytesRead;
			}
			in.close();
			out.close();

			//if total bytes to download could not be determined, set it to the actual value
			totalBytesToDownload = bytesDownloaded;
		}
		finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}

	public boolean isDownloaded() {
		return totalBytesToDownload == bytesDownloaded;
	}

	public long getBytesDownloaded() {
		return bytesDownloaded;
	}

	public long getTotalBytesToDownload() {
		return totalBytesToDownload;
	}

	public static class OclResponse {

		private final InputStream in;

		private final Date updatedTo;

		private final long contentLength;

		public OclResponse(InputStream in, long contentLength, Date updatedTo) {
			this.in = in;
			this.updatedTo = updatedTo;
			this.contentLength = contentLength;
		}

		public InputStream getContentStream() {
			return in;
		}

		public Date getUpdatedTo() {
			return updatedTo;
		}

		public long getContentLength() {
			return contentLength;
		}
	}

	public String fetchLatestOclReleaseVersion(String url, String token) throws IOException {
		url = removeLastUrlForwardSlashIfExist(url);

		String latestVersionUrl = url + "/versions";

		GetMethod versionsGet = executeGetMethod(latestVersionUrl, token);

		if (versionsGet.getStatusCode() != 200) {
			throw new IOException(versionsGet.getStatusLine().toString());
		}

		ObjectMapper objectMapper = new ObjectMapper();
		@SuppressWarnings("unchecked")
		Map<String, Object>[] versionsResponse = objectMapper.readValue(versionsGet.getResponseBodyAsStream(), Map[].class);

		for (Map<String, Object> version : versionsResponse) {
			String versionName = ((String) version.get("id"));
			if (!versionName.contains("HEAD") && StringUtils.isNotBlank(versionName)) {
				String versionUrl = url + "/" + versionName + "/export";

				GetMethod exportGet = executeGetMethod(versionUrl, token);

				int statusCode = exportGet.getStatusCode();
				if (statusCode == 200) {
					return versionName;
				}
			}
		}
		throw new IllegalStateException("There is no released version of given source");
	}

	private GetMethod constructGetMethod(String url, String token) throws IOException {
		if (!StringUtils.isBlank(token)) {
			URL oclUrl = new URL(url);
			client.getState().setCredentials(
					new AuthScope(oclUrl.getHost(), oclUrl.getPort()),
					new OclTokenCredentials(token));
		}

		return new GetMethod(url);
	}

	private GetMethod executeGetMethod(String url, String token) throws IOException {
		GetMethod getMethod = constructGetMethod(url, token);

		try {
			client.executeMethod(getMethod);
		}
		finally {
			client.getState().clearCredentials();
		}

		return getMethod;
	}

	private static final class OclTokenCredentials implements Credentials {

		private final String token;

		OclTokenCredentials(String token) {
			this.token = token;
		}

		public String getToken() {
			return token;
		}
	}

	private static final class OclTokenAuthenticationScheme implements AuthScheme {

		@Override
		public void processChallenge(String challenge) {
			// no implementation needed
		}

		@Override
		public String getSchemeName() {
			return "OCL Token Auth";
		}

		@Override
		public String getParameter(String name) {
			return null;
		}

		@Override
		public String getRealm() {
			return null;
		}

		@Override
		public String getID() {
			return null;
		}

		@Override
		public boolean isConnectionBased() {
			return false;
		}

		@Override
		public boolean isComplete() {
			return true;
		}

		@Override
		@Deprecated
		public String authenticate(Credentials credentials, String method, String uri) throws AuthenticationException {
			if (!(credentials instanceof OclTokenCredentials)) {
				throw new AuthenticationException("credentials must be an OclTokenCredentials object");
			}

			return "Token " + ((OclTokenCredentials) credentials).getToken();
		}

		@Override
		public String authenticate(Credentials credentials, HttpMethod method) throws AuthenticationException {
			if (!(credentials instanceof OclTokenCredentials)) {
				throw new AuthenticationException("credentials must be an OclTokenCredentials object");
			}

			return "Token " + ((OclTokenCredentials) credentials).getToken();
		}
	}
}
