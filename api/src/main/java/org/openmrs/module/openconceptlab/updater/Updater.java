package org.openmrs.module.openconceptlab.updater;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.client.OclClient;
import org.openmrs.module.openconceptlab.client.OclClient.OclResponse;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclMapping;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("openconceptlab.updater")
public class Updater implements Runnable {
	
	private Log log = LogFactory.getLog(getClass());
	
	public final static int BATCH_SIZE = 100;
	
	@Autowired
	UpdateService updateService;
	
	@Autowired
	OclClient oclClient;
	
	@Autowired
	Importer importer;
	
	private volatile Update update;
	
	private CountingInputStream in = null;
	
	private volatile long totalBytesToProcess;
			
	/**
	 * Runs an update for a configured subscription.
	 * <p>
	 * It is not run directly, rather by a dedicated scheduler {@link UpdateScheduler}.
	 * 
	 * @should start first update with response date
	 * @should start next update with updated since
	 * @should create item for each concept and mapping
	 */
	@Override
	public void run() {			
		runAndHandleErrors(new Task() {
			
			@Override
			public void run() throws Exception {
				Subscription subscription = updateService.getSubscription();
				Update lastUpdate = updateService.getLastSuccessfulSubscriptionUpdate();
				Date updatedSince = null;
				if (lastUpdate != null) {
					updatedSince = lastUpdate.getOclDateStarted();
				}
				
				OclResponse oclResponse;
				
				if (updatedSince == null) {
					oclResponse = oclClient.fetchInitialUpdates(subscription.getUrl(), subscription.getToken());
				} else {
					oclResponse = oclClient.fetchUpdates(subscription.getUrl(), subscription.getToken(), updatedSince);
				}
				
				updateService.updateOclDateStarted(update, oclResponse.getUpdatedTo());
				
				in = new CountingInputStream(oclResponse.getContentStream());
				totalBytesToProcess = oclResponse.getContentLength();
				
				processInput();
				
				in.close();
			}
		});
	}
	
	/**
	 * It can be used to run update from the given input e.g. from a resource bundled with a module.
	 * <p>
	 * It does not require any subscription to be setup.
	 * 
	 * @param inputStream to JSON in OCL format
	 */
	public void run(final InputStream inputStream) {
		runAndHandleErrors(new Task() {
			
			@Override
			public void run() throws IOException {
				in = new CountingInputStream(inputStream);
				
				processInput();
				
				in.close();
			}
		});
	}
		
	private interface Task {
		public void run() throws Exception;
	}
	
	private void runAndHandleErrors(Task task) {
		Update newUpdate = new Update();
		updateService.startUpdate(newUpdate);
		update = newUpdate;
		totalBytesToProcess = -1; //unknown
		
		try {
			task.run();
		} catch (Exception e) {
			update.setErrorMessage(getErrorMessage(e));
			throw new ImportException(e);
		}
		finally {
			IOUtils.closeQuietly(in);
			
			try {
				if (update != null && update.getUpdateId() != null) {
					update = updateService.getUpdate(update.getUpdateId());
					updateService.stopUpdate(update);
				}
			} catch (Exception e) {
				log.error("Failed to stop update", e);
			}
			
			in = null;
			totalBytesToProcess = 0;
			update = null;
		}
	}

	private String getErrorMessage(Exception e) {
	    String message = "Failed with '" + e.getMessage() + "'";
	    Throwable rootCause = ExceptionUtils.getRootCause(e);
	    if (rootCause != null) {
	    	String[] stackFrames = ExceptionUtils.getStackFrames(rootCause);
	    	int endIndex = stackFrames.length > 5 ? 5 : stackFrames.length;
	    	message += " caused by: " + StringUtils.join(stackFrames, "\n", 0, endIndex);
	    }
	    if (message.length() > 1024) {
	    	return message.substring(0, 1024);
	    } else {
	    	return message;
	    }
    }
	
	public long getBytesDownloaded() {
		return oclClient.getBytesDownloaded();
	}
	
	public long getTotalBytesToDownload() {
		return oclClient.getTotalBytesToDownload();
	}
	
	public boolean isDownloaded() {
		return oclClient.isDownloaded();
	}
	
	public long getBytesProcessed() {
		if (in != null) {
			return in.getByteCount();
		} else {
			return 0;
		}
	}
	
	public long getTotalBytesToProcess() {
		return totalBytesToProcess;
	}
	
	public boolean isProcessed() {
		return totalBytesToProcess == getBytesProcessed();
	}
	
	public boolean isRunning() {
		return update != null;
	}
	
	private void processInput() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonParser parser = objectMapper.getJsonFactory().createJsonParser(in);
		
		JsonToken token = parser.nextToken();
		if (token != JsonToken.START_OBJECT) {
			throw new IOException("JSON must start from an object");
		}
		
		token = advanceToListOf("concepts", parser);
		
		if (token == JsonToken.END_OBJECT) {
			return;
		}
		
		String baseUrl = updateService.getSubscription().getUrl();
		if (baseUrl != null) {
			try {
				URI uri = new URI(baseUrl);
				baseUrl = uri.getScheme() + "://" + uri.getHost();
				if (uri.getPort() != -1) {
					baseUrl += ":" + uri.getPort();
				}
			} catch (Exception e) {
				throw new IllegalStateException(baseUrl + " is not valid", e);
			}
		}
		
		int batch = 0;
		while (parser.nextToken() != JsonToken.END_ARRAY) {
			OclConcept oclConcept = parser.readValueAs(OclConcept.class);
			oclConcept.setVersionUrl(prependBaseUrl(baseUrl, oclConcept.getVersionUrl()));
			oclConcept.setUrl(prependBaseUrl(baseUrl, oclConcept.getUrl()));
			
			Item item = null;
			try {
				item = importer.importConcept(update, oclConcept);
			}
			catch (Exception e) {
				item = new Item(update, oclConcept, ItemState.ERROR);
				item.setErrorMessage(getErrorMessage(e));
			} finally {
				updateService.saveItem(item);
			}
			
			batch++;
			if (batch == BATCH_SIZE) {
				batch = 0;
				Context.flushSession();
				Context.clearSession();
			}
		}
		
		token = advanceToListOf("mappings", parser);
		
		if (token == JsonToken.END_OBJECT) {
			return;
		}
		
		batch = 0;
		while (parser.nextToken() != JsonToken.END_ARRAY) {
			OclMapping oclMapping = parser.readValueAs(OclMapping.class);
			oclMapping.setUrl(prependBaseUrl(baseUrl, oclMapping.getUrl()));
			oclMapping.setFromConceptUrl(prependBaseUrl(baseUrl, oclMapping.getFromConceptUrl()));
			oclMapping.setFromSourceUrl(prependBaseUrl(baseUrl, oclMapping.getFromSourceUrl()));
			oclMapping.setToConceptUrl(prependBaseUrl(baseUrl, oclMapping.getToConceptUrl()));
			
			Item item = null;
			try {
				item = importer.importMapping(update, oclMapping);
			}
			catch (Exception e) {
				item = new Item(update, oclMapping, ItemState.ERROR);
				item.setErrorMessage(getErrorMessage(e));
			} finally {
				updateService.saveItem(item);
			}
			
			batch++;
			if (batch == BATCH_SIZE) {
				batch = 0;
				Context.flushSession();
				Context.clearSession();
			}
		}
	}
	
	private String prependBaseUrl(String baseUrl, String url) {
		if (baseUrl == null) {
			return url;
		}
		if (url == null) {
			return null;
		}
		
		if (!url.startsWith("/")) {
			url = "/" + url;
		}
		return baseUrl + url;
	}

	private JsonToken advanceToListOf(String field, JsonParser parser) throws IOException, JsonParseException {
	    JsonToken token;
		while ((token = parser.nextToken()) != JsonToken.END_OBJECT) {
			if (parser.getText().equals(field)) {
				token = parser.nextToken();
				if (token != JsonToken.START_ARRAY) {
					throw new IOException("JSON must have a list of " + field);
				}
				break;
			}
		}
		
		return token;
    }
	
}
