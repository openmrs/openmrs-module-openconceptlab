/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.updater;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.openconceptlab.CacheService;
import org.openmrs.module.openconceptlab.OpenConceptLabActivator;
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
	
	public final static int BATCH_SIZE = 128;
	
	public final static int THREAD_POOL_SIZE = 16;
	
	@Autowired
	UpdateService updateService;
	
	@Autowired
	ConceptService conceptService;
	
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
		Daemon.runInDaemonThreadAndWait(new Runnable() {
			
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
							oclResponse = oclClient.fetchUpdates(subscription.getUrl(), subscription.getToken(),
							    updatedSince);
						}
						
						updateService.updateOclDateStarted(update, oclResponse.getUpdatedTo());
						
						in = new CountingInputStream(oclResponse.getContentStream());
						totalBytesToProcess = oclResponse.getContentLength();
						
						processInput();
						
						in.close();
					}
				});
			}
		}, OpenConceptLabActivator.getDaemonToken());
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
		}
		catch (Exception e) {
			updateService.failUpdate(update, getErrorMessage(e));
			throw new ImportException(e);
		}
		finally {
			IOUtils.closeQuietly(in);
			
			try {
				if (update != null && update.getUpdateId() != null) {
					updateService.stopUpdate(update);
				}
			}
			catch (Exception e) {
				log.error("Failed to stop update", e);
			}
			
			in = null;
			totalBytesToProcess = 0;
			update = null;
		}
	}
	
	public static String getErrorMessage(Exception e) {
		String message = "Failed with '" + e.getMessage() + "'";
		Throwable rootCause = ExceptionUtils.getRootCause(e);
		if (rootCause == null) {
			rootCause = e;
		}
		
		String[] stackFrames = ExceptionUtils.getStackFrames(rootCause);
		int endIndex = stackFrames.length > 5 ? 5 : stackFrames.length;
		message += " caused by: " + StringUtils.join(stackFrames, "\n", 0, endIndex);
		
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
		token = parser.nextToken();
		
		token = advanceToListOf("concepts", "mappings", parser);
		
		if (token == JsonToken.END_OBJECT || token == null) {
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
			}
			catch (Exception e) {
				throw new IllegalStateException(baseUrl + " is not valid", e);
			}
		}
		
		ThreadPoolExecutor runner = newRunner();
		List<OclConcept> oclConcepts = new ArrayList<OclConcept>();
		while (parser.nextToken() != JsonToken.END_ARRAY) {
			OclConcept oclConcept = parser.readValueAs(OclConcept.class);
			oclConcept.setVersionUrl(prependBaseUrl(baseUrl, oclConcept.getVersionUrl()));
			oclConcept.setUrl(prependBaseUrl(baseUrl, oclConcept.getUrl()));
			
			oclConcepts.add(oclConcept);
			
			if (oclConcepts.size() >= BATCH_SIZE) {
				ImportRunner importRunner = new ImportRunner(importer, new CacheService(conceptService), updateService,
				        update);
				importRunner.setOclConcepts(oclConcepts);
				
				oclConcepts = new ArrayList<OclConcept>();
				
				runner.execute(importRunner);
			}
		}
		
		if (oclConcepts.size() != 0) {
			ImportRunner importRunner = new ImportRunner(importer, new CacheService(conceptService), updateService, update);
			importRunner.setOclConcepts(oclConcepts);
			
			runner.execute(importRunner);
		}
		
		runner.shutdown();
		try {
			runner.awaitTermination(32, TimeUnit.DAYS);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		token = advanceToListOf("mappings", null, parser);
		
		if (token == JsonToken.END_OBJECT) {
			return;
		}
		
		runner = newRunner();
		List<OclMapping> oclMappings = new ArrayList<OclMapping>();
		while (parser.nextToken() != JsonToken.END_ARRAY) {
			OclMapping oclMapping = parser.readValueAs(OclMapping.class);
			oclMapping.setUrl(prependBaseUrl(baseUrl, oclMapping.getUrl()));
			oclMapping.setFromConceptUrl(prependBaseUrl(baseUrl, oclMapping.getFromConceptUrl()));
			oclMapping.setFromSourceUrl(prependBaseUrl(baseUrl, oclMapping.getFromSourceUrl()));
			oclMapping.setToConceptUrl(prependBaseUrl(baseUrl, oclMapping.getToConceptUrl()));
			
			oclMappings.add(oclMapping);
			
			if (oclMappings.size() >= BATCH_SIZE) {
				ImportRunner importRunner = new ImportRunner(importer, new CacheService(conceptService), updateService,
				        update);
				importRunner.setOclMappings(oclMappings);
				
				oclMappings = new ArrayList<OclMapping>();
				
				runner.execute(importRunner);
			}
		}
		
		if (oclMappings.size() != 0) {
			ImportRunner importRunner = new ImportRunner(importer, new CacheService(conceptService), updateService, update);
			importRunner.setOclMappings(oclMappings);
			
			runner.execute(importRunner);
		}
		
		runner.shutdown();
		try {
			runner.awaitTermination(32, TimeUnit.DAYS);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	private ThreadPoolExecutor newRunner() {
		return new ThreadPoolExecutor(0, THREAD_POOL_SIZE, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(
		        THREAD_POOL_SIZE / 2), new RejectedExecutionHandler() {
					
					@Override
					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
						try {
	                        executor.getQueue().put(r);
                        }
                        catch (InterruptedException e) {
                        	throw new RejectedExecutionException("Work discarded", e);
                        }
					}
				});
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
	
	private JsonToken advanceToListOf(String field, String stopAtField, JsonParser parser) throws IOException,
	        JsonParseException {
		JsonToken token = parser.getCurrentToken();
		if (token == null) {
			token = parser.nextToken();
		}
		
		do {
			if (token == JsonToken.START_OBJECT) {
				String text = parser.getText();
				while ((token = parser.nextToken()) != JsonToken.END_OBJECT) {
					if (token == null) {
						throw new IOException("Missing end of object: " + text);
					}
				}
			} else if (parser.getText().equals(field)) {
				token = parser.nextToken();
				if (token != JsonToken.START_ARRAY) {
					throw new ImportException(field + " must be a list");
				}
				return token;
			} else if (token == JsonToken.START_ARRAY) {
				String text = parser.getText();
				while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
					if (token == null) {
						throw new IOException("Missing end of array: " + text);
					}
				}
			} else if (stopAtField != null && parser.getText().equals(stopAtField)) {
				return token;
			}
		} while ((token = parser.nextToken()) != null);
		
		return null;
	}
	
}
