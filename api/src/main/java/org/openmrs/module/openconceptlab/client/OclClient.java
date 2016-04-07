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
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.util.OpenmrsUtil;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
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

	private HttpClient httpClient;

	private int page = 1;
	private boolean isPaging;
	private OclResponse oclResponse;

	private String lastRequestURL;
	private String lastRequestToken;
	private Date lastRequestDate;
	
	public OclClient() {
		dataDirectory = OpenmrsUtil.getApplicationDataDirectory();
		httpClient = new HttpClient();
	}
	
	public OclClient(String dataDirectory) {
		this.dataDirectory = dataDirectory;
		httpClient = new HttpClient();
	}

	public OclClient(HttpClient httpClient) {
		this.dataDirectory = OpenmrsUtil.getApplicationDataDirectory();
		this.httpClient = httpClient;
	}

	public OclResponse fetchUpdates(String url, String token, Date updatedSince) throws IOException {
		totalBytesToDownload = -1; //unknown yet
		bytesDownloaded = 0;

		GetMethod get = newGetMethod(url, token, updatedSince);

		return fetchUpdates(get);
	}

	public OclResponse fetchUpdates(GetMethod get) throws IOException {
		httpClient.getHttpConnectionManager().getParams().setSoTimeout(TIMEOUT_IN_MS);
		httpClient.executeMethod(get);

		if (get.getStatusCode() != 200) {
			throw new IOException(get.getStatusLine().toString());
		}

		isPaging = false;
		OclResponse initResponse = extractResponse(get);
		InputStream contentStream = initResponse.getContentStream();

		while (true) {
			int firstByteOfResponse = contentStream.read();
			if (firstByteOfResponse != -1) {
				isPaging = true;
				get.setQueryString(get.getQueryString().replace("&page=" + String.valueOf(page),"&page=" + String.valueOf(++page)));
				httpClient.executeMethod(get);
				if (get.getStatusCode() != 200) {
					throw new IOException(get.getStatusLine().toString());
				}
				OclResponse response = extractResponse(get);
				if (response != null) {
					contentStream = response.getContentStream();
				}
				else {
					break;
				}
			}
		}
		isPaging = false;
		page = 1;

		return oclResponse;
	}

	private GetMethod newGetMethod(String url, String token, Date updatedSince) {

		lastRequestURL = url;
		lastRequestToken = token;
		lastRequestDate = updatedSince;

		GetMethod get = new GetMethod(url);
		if (!StringUtils.isBlank(token)) {
			get.addRequestHeader("Authorization", "Token " + token);
			get.addRequestHeader("Compress", "true");
		}

		List<NameValuePair> query = new ArrayList<NameValuePair>();
		query.add(new NameValuePair("includeMappings", "true"));
		query.add(new NameValuePair("includeConcepts", "true"));
		query.add(new NameValuePair("includeRetired", "true"));
		query.add(new NameValuePair("limit", "1000"));
		query.add(new NameValuePair("page", String.valueOf(page)));


		if (updatedSince != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			query.add(new NameValuePair("updatedSince", dateFormat.format(updatedSince)));
		}

		get.setQueryString(query.toArray(new NameValuePair[0]));
		return get;
	}


	public OclResponse fetchInitialUpdates(String url, String token) throws IOException, HttpException {
		totalBytesToDownload = -1; //unknown yet
		bytesDownloaded = 0;
		
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		
		String latestVersion = fetchLatestVersion(url, token);
		
		String exportUrl = fetchExportUrl(url, token, latestVersion);
		
		GetMethod exportUrlGet = new GetMethod(exportUrl);
		
		HttpClient client = new HttpClient();
		client.getHttpConnectionManager().getParams().setSoTimeout(TIMEOUT_IN_MS);
		client.executeMethod(exportUrlGet);
		
		if (exportUrlGet.getStatusCode() != 200) {
			throw new IOException(exportUrlGet.getStatusLine().toString());
		}
		
		return extractResponse(exportUrlGet);
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
    public OclResponse ungzipAndUntarResponse(InputStream response, Date date) throws IOException {
		GZIPInputStream gzipIn = new GZIPInputStream(response);
		TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn);
		boolean foundEntry = false;
		try {
			TarArchiveEntry entry = tarIn.getNextTarEntry();
			while (entry != null) {
				if (entry.getName().equals("export.json")) {
					foundEntry = true;
					if (oclResponse == null) {
						oclResponse =  new OclResponse(tarIn, entry.getSize(), date);
						return oclResponse;
					}
					else {
						oclResponse.addNextPage(tarIn, entry.getSize());
						return oclResponse;
					}
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
	public OclResponse unzipResponse(InputStream response, Date date) throws IOException {
		ZipInputStream zip = new ZipInputStream(response);
		boolean foundEntry = false;
		try {
			ZipEntry entry = zip.getNextEntry();
			while (entry != null) {
				if (entry.getName().equals("export.json")) {
					foundEntry = true;
					if (oclResponse == null) {
						oclResponse =  new OclResponse(zip, entry.getSize(), date);
						return oclResponse;
					}
					else {
						oclResponse.addNextPage(zip, entry.getSize());
						return oclResponse;
					}
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

		return null;
	//	throw new IOException("Unsupported format of response. Expected zip with export.json.");
	}
	
	File newFile(Date date) {
		SimpleDateFormat fileNameFormat = new SimpleDateFormat(FILE_NAME_FORMAT);
		String fileName = fileNameFormat.format(date) + ".zip";
		File oclDirectory = new File(dataDirectory, "ocl");
		if (!oclDirectory.exists()) {
			oclDirectory.mkdirs();
		}
		File file = new File(oclDirectory, fileName);
		return file;
	}
	
	void download(InputStream in, long length, File destination) {
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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			//e.printStackTrace();
		} finally {
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


		private final Date updatedTo;

		private long contentLength = 0;

		private LinkedList<InputStream> inputStreams = new LinkedList<InputStream>();
		private ListIterator<InputStream> listIterator = inputStreams.listIterator();

		public OclResponse(InputStream in, long contentLength, Date updatedTo) throws IOException {
			addNextPage(in, contentLength);
			this.updatedTo = updatedTo;
		}

		public void addNextPage(InputStream inputStream, long contentLength) throws IOException {

			if (!isInputStreamEmpty(inputStream)) {
				// Do I need list for this one to separate?
				this.contentLength += contentLength;
			}
		}

		private boolean isInputStreamEmpty(InputStream in) {

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			// TODO: extract substring!!!

			for (int i = 0; i < 3; i++) {
				try {
					baos.write(in.read());
				} catch (IOException e) {
					//e.printStackTrace();
					break;
				}
			}

			String string = baos.toString();
			boolean isEmpty = string.substring(0,2).equals("{}");

			if (isEmpty) {
				return false;
			}
			else {
				inputStreams.add(in);
				return true;
			}
		}

		public boolean hasNextPage() {
			return listIterator.hasNext();
		}

		public InputStream getContentStream() {
			if (hasNextPage()) {
				return inputStreams.get(listIterator.nextIndex());
			}
			else {
				return inputStreams.getLast();
			}
		}

		public LinkedList<InputStream> getContentStreams() {
			return inputStreams;
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
		if (latestVersionExportUrlGet.getStatusCode() != 200) {
			throw new IOException(latestVersionExportUrlGet.getPath() + " responded with " + latestVersionExportUrlGet.getStatusLine().toString());
		}
		
		String exportUrl = latestVersionExportUrlGet.getResponseHeader("exportURL").getValue();
	    return exportUrl;
    }

	private String fetchLatestVersion(String url, String token) throws IOException, HttpException, JsonParseException,
            JsonMappingException {
	    String latestVersionUrl = url + "/latest";
		
		GetMethod latestVersionGet = new GetMethod(latestVersionUrl);
		if (!StringUtils.isBlank(token)) {
			latestVersionGet.addRequestHeader("Authorization", "Token " + token);
		}
		
		HttpClient client = new HttpClient();
		client.getHttpConnectionManager().getParams().setSoTimeout(TIMEOUT_IN_MS);
		client.executeMethod(latestVersionGet);
		if (latestVersionGet.getStatusCode() != 200) {
			throw new IOException(latestVersionGet.getStatusLine().toString());
		}
		
		ObjectMapper objectMapper = new ObjectMapper();
		@SuppressWarnings("unchecked")
        Map<String, Object> latestVersionResponse = objectMapper.readValue(latestVersionGet.getResponseBodyAsStream(), Map.class);
		String latestVersion = (String) latestVersionResponse.get("id");
	    return latestVersion;
    }
}
