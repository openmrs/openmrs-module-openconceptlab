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
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
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
	
	private final String dataDirectory;
	
	private final int bufferSize = 64 * 1024;
	
	public final static int TIMEOUT_IN_MS = 128000;
	
	private volatile long bytesDownloaded = 0;
	
	private volatile long totalBytesToDownload = 0;
	
	public OclClient() {
		dataDirectory = OpenmrsUtil.getApplicationDataDirectory();
	}
	
	public OclClient(String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}
	
	public OclResponse fetchSnapshotUpdates(String url, String token, Date updatedSince) throws IOException {
		totalBytesToDownload = -1; //unknown yet
		bytesDownloaded = 0;
		
		GetMethod get = new GetMethod(url);
		if (!StringUtils.isBlank(token)) {
			get.addRequestHeader("Authorization", "Token " + token);
			get.addRequestHeader("Compress", "true");
		}
		
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
		
		HttpClient client = new HttpClient();
		client.getHttpConnectionManager().getParams().setSoTimeout(TIMEOUT_IN_MS);
		client.executeMethod(get);
		
		if (get.getStatusCode() != 200) {
			throw new IOException(get.getStatusLine().toString());
		}
		
		return extractResponse(get);
	}
	
	public OclResponse fetchLastReleaseVersion(String url, String token) throws IOException {
		totalBytesToDownload = -1; //unknown yet
		bytesDownloaded = 0;

		String latestVersion = fetchLatestOclReleaseVersion(url, token);

		GetMethod exportUrlGet = executeExportRequest(url, latestVersion);
		
		return extractResponse(exportUrlGet);
    }

    public GetMethod executeExportRequest(String url, String latestVersion) throws IOException{

		GetMethod exportUrlGet = new GetMethod(url + "/" + latestVersion + "/export");

		HttpClient client = new HttpClient();
		client.getHttpConnectionManager().getParams().setSoTimeout(TIMEOUT_IN_MS);
		client.executeMethod(exportUrlGet);

		if (exportUrlGet.getStatusCode() != 200) {
			throw new IOException(exportUrlGet.getStatusLine().toString());
		}

		return exportUrlGet;
	}

	public OclResponse fetchLastReleaseVersion(String url, String token, String lastReleaseVersion) throws IOException {
		String latestOclReleaseVersion = fetchLatestOclReleaseVersion(url, token);
		if (lastReleaseVersion == null || !lastReleaseVersion.equals(latestOclReleaseVersion)) {
			//If there is no lastReleaseVersion then the subscription has been changed from snapshot to releases
			//and we need to fetch the latest OCL release version.
			//If lastReleaseVersion does not match the latest OCL release version then we need to fetch it too.
			return fetchLastReleaseVersion(url, token);
		}
		else {
			//No new version
			return null;
		}
	}

	/**
	 * @should extract date and json
	 */
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
	    Pattern pattern = Pattern.compile("[0-9]*.tgz");
	    Matcher matcher = pattern.matcher(path);
	    if (matcher.find()) {
	    	String foundDate = matcher.group();
	    	foundDate = foundDate.substring(0, foundDate.indexOf(".tgz"));
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
		} finally {
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
		File file = new File(oclDirectory, fileName);
		return file;
	}
	
	void download(InputStream in, long length, File destination) throws IOException {
		OutputStream out = null;
		try {
			totalBytesToDownload = length;
			bytesDownloaded = 0;
			
			out = new BufferedOutputStream(new FileOutputStream(destination), bufferSize);
			
			byte[] buffer = new byte[bufferSize];
			int bytesRead = 0;
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

	private String fetchExportUrl(String url, String token, String latestVersion) throws IOException, HttpException {
	    String latestVersionExportUrl = url + "/" + latestVersion + "/export";

		GetMethod latestVersionExportUrlGet = new GetMethod(latestVersionExportUrl);
		if (!StringUtils.isBlank(token)) {
			latestVersionExportUrlGet.addRequestHeader("Authorization", "Token " + token);
		}

		HttpClient client = new HttpClient();
		client.getHttpConnectionManager().getParams().setSoTimeout(TIMEOUT_IN_MS);
		client.executeMethod(latestVersionExportUrlGet);
		if (latestVersionExportUrlGet.getStatusCode() != 303) {
			throw new IOException(latestVersionExportUrlGet.getPath() + " responded with " + latestVersionExportUrlGet.getStatusLine().toString());
		}

		return latestVersionExportUrlGet.getResponseHeader("Location").getValue();
    }

	public String fetchLatestOclReleaseVersion(String url, String token) throws IOException {
		if (url.endsWith("/")) {
			url = url.substring(0, url.lastIndexOf('/'));
		}

	    String latestVersionUrl = url + "/versions";
		
		GetMethod versionsGet = new GetMethod(latestVersionUrl);
		if (!StringUtils.isBlank(token)) {
			versionsGet.addRequestHeader("Authorization", "Token " + token);
		}
		
		HttpClient versionsClient = new HttpClient();
		versionsClient.getHttpConnectionManager().getParams().setSoTimeout(TIMEOUT_IN_MS);
		versionsClient.executeMethod(versionsGet);
		if (versionsGet.getStatusCode() != 200) {
			throw new IOException(versionsGet.getStatusLine().toString());
		}
		
		ObjectMapper objectMapper = new ObjectMapper();
		@SuppressWarnings("unchecked")
        Map<String, Object>[] versionsResponse = objectMapper.readValue(versionsGet.getResponseBodyAsStream(), Map[].class);

		for (Map version : versionsResponse) {
			String versionName = ((String) version.get("id"));
			if (!versionName.contains("HEAD") && StringUtils.isNotBlank(versionName)) {

				GetMethod exportGet = new GetMethod(url + "/" + versionName + "/export");

				HttpClient exportClient = new HttpClient();
				exportClient.getHttpConnectionManager().getParams().setSoTimeout(TIMEOUT_IN_MS);
				try {
					exportClient.executeMethod(exportGet);
				} catch (IOException e) {
					throw new IllegalStateException("Couldn't execute GET to " + url + versionName + "/export");
				}

				int statusCode = exportGet.getStatusCode();
				if (statusCode == 200) {
					return versionName;
				}
			}
		}
		throw new IllegalStateException("There is no released version of given source");
    }

}
