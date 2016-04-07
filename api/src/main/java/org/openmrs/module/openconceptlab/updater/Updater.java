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
import org.openmrs.module.openconceptlab.*;
import org.openmrs.module.openconceptlab.client.OclClient;
import org.openmrs.module.openconceptlab.client.OclClient.OclResponse;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclMapping;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

public class Updater implements Runnable {

	private Log log = LogFactory.getLog(getClass());

	public final static int BATCH_SIZE = 128;

	public final static int THREAD_POOL_SIZE = 16;

	UpdateService updateService;

	ConceptService conceptService;

	OclClient oclClient;

	Importer importer;

	private volatile Update update;

	private CountingInputStream in = null;

	private volatile long totalBytesToProcess;


    public void setUpdateService(UpdateService updateService) {
	    this.updateService = updateService;
    }


    public void setConceptService(ConceptService conceptService) {
	    this.conceptService = conceptService;
    }


    public void setOclClient(OclClient oclClient) {
	    this.oclClient = oclClient;
    }

    public void setImporter(Importer importer) {
	    this.importer = importer;
    }

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

						OclResponse oclResponse = getOclResponse();

						if (oclResponse.hasNextPage()) {
							for (InputStream inputStream : oclResponse.getContentStreams()) {
								updateService.updateOclDateStarted(update, oclResponse.getUpdatedTo());

								in = new CountingInputStream(oclResponse.getContentStream());
								totalBytesToProcess = oclResponse.getContentLength();

								processInput();

								in.close();
							}
						}

					}
				});
			}
		}, OpenConceptLabActivator.getDaemonToken());
	}

	public OclResponse getOclResponse() throws IOException {
		Subscription subscription = updateService.getSubscription();
		Update lastUpdate = updateService.getLastSuccessfulSubscriptionUpdate();
		Date updatedSince = null;
		if (lastUpdate != null) {
			updatedSince = lastUpdate.getOclDateStarted();
		}

		if (updatedSince == null) {
			return oclClient.fetchInitialUpdates(subscription.getUrl(), subscription.getToken());
		} else {
			return oclClient.fetchUpdates(subscription.getUrl(), subscription.getToken(),
					updatedSince);
		}
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

			Integer errors = updateService.getUpdateItemsCount(update, new HashSet<ItemState>(Arrays.asList(ItemState.ERROR)));
			if (errors > 0) {
				updateService.failUpdate(update);
			}
		}
		catch (Throwable e) {
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

	public static String getErrorMessage(Throwable e) {
		String message = e.getMessage();
		Throwable rootCause = ExceptionUtils.getRootCause(e);
		if (rootCause == null) {
			rootCause = e;
		}

		String[] stackFrames = ExceptionUtils.getStackFrames(rootCause);
		int endIndex = stackFrames.length > 5 ? 5 : stackFrames.length;
		message += "\n caused by: " + StringUtils.join(stackFrames, "\n", 0, endIndex);

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
		Update lastUpdate = updateService.getLastUpdate();
		if (lastUpdate == null) {
			return false;
		}

		if (update == null && !lastUpdate.isStopped()) {
			lastUpdate.setErrorMessage("Process terminated before completion");
			updateService.stopUpdate(lastUpdate);
		}

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

	public UpdateProgress getUpdateProgress() {
		UpdateProgress updateProgress = new UpdateProgress();

		Update lastUpdate = updateService.getLastUpdate();
		long time = (new Date().getTime() - lastUpdate.getLocalDateStarted().getTime()) / 1000;
		updateProgress.setTime(time);

		if (!isDownloaded()) {
			double totalBytesToDownload = getTotalBytesToDownload();
			double progress = 0;
			if (getBytesDownloaded() == 0) {
				//simulate download progress until first bytes are downloaded
				progress = (double) time / (time + 5) * 10.0;
			} else if (getTotalBytesToDownload() == -1) {
				//simulate download progress since total bytes to download are unknown
				progress = 10.0 + ((double) time / (time + 100) * 20.0);
			} else {
				progress = 10.0 + ((double) getBytesDownloaded() / totalBytesToDownload * 20.0);
			}
			updateProgress.setProgress((int) progress);
		} else if (!isProcessed()) {
			double progress = 30;
			if (getTotalBytesToProcess() == -1) {
				progress = 30 + ((double) time / (time + 100) * 70.0);
			} else {
				progress = 30.0 + ((double) getBytesProcessed() / getTotalBytesToProcess() * 70.0);
			}
			updateProgress.setProgress((int) progress);
		} else {
			updateProgress.setProgress(100);
		}

		return updateProgress;
	}

}
