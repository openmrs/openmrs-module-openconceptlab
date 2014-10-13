package org.openmrs.module.openconceptlab;

import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.stereotype.Component;

@Component
public class OclClient {
	
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	
	public OclResponse fetchUpdates(String url, Date updatedSince) throws IOException {
		GetMethod get = new GetMethod(url);
		get.addRequestHeader(new Header("compress", "true"));
		get.getParams().setParameter("verbose", true);
		get.getParams().setParameter("includeRetired", true);
		
		if (updatedSince != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			get.getParams().setParameter("updatedSince", dateFormat.format(updatedSince));
		}
		
		new HttpClient().executeMethod(get);
		
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
		
		String json = "";
		InputStream response = get.getResponseBodyAsStream();		
		ZipInputStream zip = new ZipInputStream(response);
		try {
			ZipEntry entry = zip.getNextEntry(); 
			while(entry != null) {
		        if (entry.getName().equals("export.json")) {
		        	json = IOUtils.toString(zip, "utf-8");
		        }
		        entry = zip.getNextEntry();
	        }
			zip.close();
		} finally {
			IOUtils.closeQuietly(zip);
		}
		
		return new OclResponse(json, date);
    }
	
	public static class OclResponse {
		
		private final String json;
		
		private final Date updatedTo;
		
		public OclResponse(String json, Date updatedTo) {
			this.json = json;
			this.updatedTo = updatedTo;
		}
		
		public String getJson() {
			return json;
		}
		
		public Date getUpdatedTo() {
			return updatedTo;
		}
		
	}
}
