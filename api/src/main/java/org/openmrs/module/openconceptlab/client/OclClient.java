package org.openmrs.module.openconceptlab.client;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.stereotype.Component;

@Component("openconceptlab.oclClient")
public class OclClient {
	
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	
	public static final String FILE_NAME_FORMAT = "yyyyMMdd_HHmmss";
	
	private final String dataDirectory;
	
	private final int bufferSize = 64 * 1024;
	
	private volatile long bytesDownloaded = 0;
	
	private volatile long totalBytesToDownload = 0;
			
	public OclClient() {
		dataDirectory = OpenmrsUtil.getApplicationDataDirectory();
	}
	
	public OclClient(String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}
	
	public OclResponse fetchUpdates(String url, String token, Date updatedSince) throws IOException {
		GetMethod get = new GetMethod(url);
		if (!StringUtils.isBlank(token)) {
			get.addRequestHeader("token", token);
		}
		get.getParams().setParameter("format", "zip");
		get.getParams().setParameter("verbose", true);
		get.getParams().setParameter("includeRetired", true);
		
		if (updatedSince != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			get.getParams().setParameter("updatedSince", dateFormat.format(updatedSince));
		}
		
		HttpClient client = new HttpClient();
		client.getHttpConnectionManager().getParams().setSoTimeout(30000);
		client.executeMethod(get);
		
		return extractResponse(get);
	}
	
	/**
	 * @should extract date and json
	 */
	OclResponse extractResponse(GetMethod get) throws IOException {
		Header dateHeader = get.getResponseHeader("date");
		Date date;
		try {
			date = DateUtil.parseDate(dateHeader.getValue());
		}
		catch (DateParseException e) {
			throw new IOException("Cannot parse date header", e);
		}
		
		File file = newFile(date);
		download(get.getResponseBodyAsStream(), get.getResponseContentLength(), file);
		
		InputStream response = new FileInputStream(file);
		return unzipResponse(response, date);
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
					return new OclResponse(zip, entry.getSize(), date);
				}
				entry = zip.getNextEntry();
			}
			
			zip.close();
		} finally {
			if (!foundEntry) {
				IOUtils.closeQuietly(zip);
			}
		}
	    throw new IOException("Unsupported format of response. Expected zip with export.json.");
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
}
