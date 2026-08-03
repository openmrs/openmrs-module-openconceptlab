/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.importer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.openconceptlab.CacheService;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportProgress;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.OclConceptService;
import org.openmrs.module.openconceptlab.OpenConceptLabActivator;
import org.openmrs.module.openconceptlab.OpenConceptLabConstants;
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.Utils;
import org.openmrs.module.openconceptlab.ValidationType;
import org.openmrs.module.openconceptlab.client.OclClient;
import org.openmrs.module.openconceptlab.client.OclClient.OclResponse;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclMapping;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipFile;

public class Importer implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(Importer.class);

	// Number of items to process before flushing/clearing the Hibernate session.
	public final static int BATCH_SIZE = 512;
	private static final String CONCEPTS_FIELD = "concepts";
	private static final String MAPPINGS_FIELD = "mappings";
	private static final String COLLECTION_FIELD = "collection";
	private static final String CUSTOM_VALIDATION_SCHEMA_FIELD = "custom_validation_schema";

	private ImportService importService;

	private ConceptService conceptService;
	
	private OclConceptService oclConceptService;

	private OclClient oclClient;

	private Saver saver;

	private volatile Import anImport;

	private CountingInputStream in = null;

	private volatile long totalBytesToProcess;

	private ZipFile zipFile;

	private interface Task {
		void run() throws Exception;
	}

    public void setImportService(ImportService importService) {
	    this.importService = importService;
    }

    public void setConceptService(ConceptService conceptService) {
	    this.conceptService = conceptService;
    }
    
    public void setOclConceptService(OclConceptService oclConceptService) {
		this.oclConceptService = oclConceptService;
    }

    public void setOclClient(OclClient oclClient) {
	    this.oclClient = oclClient;
    }

    public void setSaver(Saver persister) {
	    this.saver = persister;
    }

	public void setZipFile(ZipFile zipFile) {
		this.zipFile = zipFile;
	}

	/**
	 * Runs an import for a configured subscription.
	 * <p>
	 * It is not run directly, rather by a dedicated scheduler {@link UpdateScheduler}.
	 * if input stream is already defined from file, subscription logic is skipped by calling run(InputStream).
	 *
	 * @should start first import with response date
	 * @should start next import with updated since
	 * @should create item for each concept and mapping
	 */
	@Override
	public void run() {
		if (zipFile != null) {
			run(zipFile);
		} else {
			Daemon.runInDaemonThreadAndWait(new Runnable() {
				@Override
				public void run() {
					runTask();
				}
			}, OpenConceptLabActivator.getDaemonToken());
		}
	}

    public void runTask() {
        runAndHandleErrors(new Task() {

            @Override
            public void run() throws Exception {
                Subscription subscription = importService.getSubscription();
                Import lastImport = importService.getLastSuccessfulSubscriptionImport();
                Date updatedSince = null;
                if (lastImport != null) {
                    updatedSince = lastImport.getOclDateStarted();
                }

                OclResponse oclResponse;

                if (updatedSince == null) {
                    oclResponse = oclClient.fetchOclConcepts(subscription.getUrl(), subscription.getToken());
					importService.updateReleaseVersion(anImport,
                            oclClient.getOclReleaseVersion(subscription.getUrl(), subscription.getToken()));
                } else {
                    if (subscription.isSubscribedToSnapshot()) {
                        oclResponse = oclClient.fetchSnapshotUpdates(subscription.getUrl(), subscription.getToken(),
                                updatedSince);
                    }
                    else {
                        oclResponse = oclClient.fetchOclConcepts(subscription.getUrl(), subscription.getToken(), lastImport.getReleaseVersion());
						importService.updateReleaseVersion(anImport,
                                oclClient.getOclReleaseVersion(subscription.getUrl(), subscription.getToken()));
                    }
                }

				if (oclResponse != null) {
					importService.updateOclDateStarted(anImport, oclResponse.getUpdatedTo());
					importService.updateSubscriptionUrl(anImport, subscription.getUrl());
					in = new CountingInputStream(oclResponse.getContentStream());
					totalBytesToProcess = oclResponse.getContentLength();

					processInput();

					in.close();
				}
            }
        });
    }

	/**
	 * This run is used to update sources from zip file
	 */
	public void run(final ZipFile zipPackage) {
		Daemon.runInDaemonThreadAndWait(() -> runAndHandleErrors(() -> {
            InputStream zipInputStream = Utils.extractExportInputStreamFromZip(zipPackage);
            try {
                in = new CountingInputStream(zipInputStream);
                String subscriptionUrl = Context.getAdministrationService()
                        .getGlobalProperty(OpenConceptLabConstants.GP_OCL_LOAD_AT_STARTUP_PATH);
                importService.updateSubscriptionUrl(anImport, subscriptionUrl);
                processInput();
            } finally {
                in.close();
            }
        }), OpenConceptLabActivator.getDaemonToken());
	}

	private void runAndHandleErrors(Task task) {
		Import newUpdate = new Import();
		importService.startImport(newUpdate);
		anImport = newUpdate;
		totalBytesToProcess = -1; //unknown

		try {
			task.run();

			Integer errors = importService.getImportItemsCount(anImport, new HashSet<>(
					Collections.singletonList(ItemState.ERROR)));
			if (errors > 0) {
				importService.failImport(anImport);
			} else {
				if (zipFile != null) {
					File file = new File(zipFile.getName());
					if (file.exists()) {
						file.delete();
					}
				}
			}
		}
		catch (Throwable e) {
			importService.failImport(anImport, getErrorMessage(e));
			throw new ImportException(e);
		}
		finally {
			IOUtils.closeQuietly(in);

			zipFile = null;

			try {
				if (anImport != null && anImport.getImportId() != null) {
					importService.stopImport(anImport);
				}
			}
			catch (Exception e) {
				log.error("Failed to stop anImport", e);
			}

			in = null;
			totalBytesToProcess = 0;
			anImport = null;
		}
	}

	public static String getErrorMessage(Throwable e) {
		String message = e.getMessage();
		Throwable rootCause = ExceptionUtils.getRootCause(e);
		if (rootCause == null) {
			rootCause = e;
		}

		String[] stackFrames = ExceptionUtils.getStackFrames(rootCause);
		int endIndex = Math.min(stackFrames.length, 5);
		message += "\n caused by: " + StringUtils.join(stackFrames, "\n", 0, endIndex);

		if (message.length() > 1024) {
			return message.substring(0, 1024);
		} else {
			return message;
		}
	}
	
	public static String getUserFriendlyErrorMessage(Throwable e) {
		String message = e.getMessage();
		Throwable rootCause = ExceptionUtils.getRootCause(e);
		if (rootCause != null) {
			message += " [CAUSE]: " + rootCause.getMessage();
		}

		return message;
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
		Import lastUpdate = importService.getLastImport();
		if (lastUpdate == null) {
			return false;
		}

		if (anImport == null && !lastUpdate.isStopped()) {
			lastUpdate.setErrorMessage("Process terminated before completion");
			importService.stopImport(lastUpdate);
		}

		return anImport != null;
	}

	private void processInput() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.getDeserializationConfig().setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
		JsonParser parser = objectMapper.getJsonFactory().createJsonParser(in);

		JsonToken token = parser.nextToken();
		if (token != JsonToken.START_OBJECT) {
			throw new IOException("JSON must start from an object");
		}

		verifyCollectionUsesOpenMRSValidationSchema(parser);

		token = advanceToListOf(CONCEPTS_FIELD, MAPPINGS_FIELD, parser);

		// Look up subscription once for the entire import
		Subscription subscription = importService.getSubscription();

		String baseUrl = "";
		if (subscription != null) {
			baseUrl = subscription.getUrl();
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
		}

		CacheService cacheService = new CacheService(conceptService, oclConceptService);

		// Determine validation type once for the entire import to avoid per-concept getSubscription() calls.
		ValidationType validationType = ValidationType.FULL;
		if (subscription != null && subscription.getValidationType() != null) {
			validationType = subscription.getValidationType();
		}

		// If this is the first-ever import, skip DB lookups for previous items since none exist.
		// We check <= 1 because the current in-progress import may already be visible in the
		// query results (Hibernate auto-flushes before Criteria queries). getImportsInOrder()
		// returns ALL imports (no status filtering), so size 0 or 1 both mean no prior import.
		List<Import> previousImports = importService.getImportsInOrder(0, 2);
		if (previousImports.size() <= 1) {
			cacheService.setSkipDbItemLookups(true);
		}

		List<Item> items = new ArrayList<>(BATCH_SIZE);

		if (token == JsonToken.START_ARRAY) {
			while (parser.nextToken() != JsonToken.END_ARRAY) {
				OclConcept oclConcept = parser.readValueAs(OclConcept.class);
				oclConcept.setVersionUrl(prependBaseUrl(baseUrl, oclConcept.getVersionUrl()));
				oclConcept.setUrl(prependBaseUrl(baseUrl, oclConcept.getUrl()));

				Item item;
				try {
					item = saver.saveConcept(cacheService, anImport, oclConcept, validationType);
					cacheService.cacheItem(item);
					log.debug("Imported concept {}", oclConcept);
				} catch (Throwable e) {
					log.error("Failed to import concept {}", oclConcept, e);
					Context.clearSession();
					cacheService.clearCache();

					item = new Item(anImport, oclConcept, ItemState.ERROR);
					item.setErrorMessage(getUserFriendlyErrorMessage(e));
				}
				items.add(item);

				if (items.size() >= BATCH_SIZE) {
					importService.saveItems(items);
					items = new ArrayList<>(BATCH_SIZE);
					// Flush and clear session to prevent memory buildup, then clear cache since entities are detached
					importService.flushAndClearSession();
					cacheService.clearCache();
				}
			}

			if (!items.isEmpty()) {
				importService.saveItems(items);
				items = new ArrayList<>(BATCH_SIZE);
			}

			// Flush and clear before processing mappings to start fresh
			importService.flushAndClearSession();
			cacheService.clearCache();
		}

		token = advanceToListOf(MAPPINGS_FIELD, null, parser);

		if (token != JsonToken.START_ARRAY) {
			return;
		}

		while (parser.nextToken() != JsonToken.END_ARRAY) {
			OclMapping oclMapping = parser.readValueAs(OclMapping.class);
			oclMapping.setUrl(prependBaseUrl(baseUrl, oclMapping.getUrl()));
			oclMapping.setFromConceptUrl(prependBaseUrl(baseUrl, oclMapping.getFromConceptUrl()));
			oclMapping.setFromSourceUrl(prependBaseUrl(baseUrl, oclMapping.getFromSourceUrl()));
			oclMapping.setToConceptUrl(prependBaseUrl(baseUrl, oclMapping.getToConceptUrl()));

			Item item;
			try {
				item = saver.saveMapping(cacheService, anImport, oclMapping);
				cacheService.cacheItem(item);
				log.debug("Imported mapping {}", oclMapping);
			} catch (SavingException e) {
				log.error("Failed to save mapping {}", oclMapping, e);
				Context.clearSession();
				cacheService.clearCache();

				item = new Item(anImport, oclMapping, ItemState.ERROR, e.getMessage());
			} catch (Throwable e) {
				log.error("Failed to import mapping {}", oclMapping, e);
				Context.clearSession();
				cacheService.clearCache();

				item = new Item(anImport, oclMapping, ItemState.ERROR);
				item.setErrorMessage(getUserFriendlyErrorMessage(e));
			}
			items.add(item);

			if (items.size() >= BATCH_SIZE) {
				importService.saveItems(items);
				items = new ArrayList<>(BATCH_SIZE);
				// Flush and clear session to prevent memory buildup, then clear cache since entities are detached
				importService.flushAndClearSession();
				cacheService.clearCache();
			}
		}

		if (!items.isEmpty()) {
			importService.saveItems(items);
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

	/**
	 * Verifies that a collection export declares the OpenMRS validation schema before the import
	 * processes its concepts and mappings. Source exports (no "collection" object in the metadata)
	 * are not checked.
	 * <p>
	 * The metadata block always precedes the "concepts"/"mappings" arrays, so only the leading
	 * fields of the top-level object are scanned. The parser is left at the "concepts" or
	 * "mappings" field name (or at the end of the object when neither exists) so that
	 * {@link #advanceToListOf(String, String, JsonParser)} can continue from where this method
	 * stopped.
	 *
	 * @throws ImportException if a collection export is not validated with the OpenMRS schema
	 */
	private void verifyCollectionUsesOpenMRSValidationSchema(JsonParser parser) throws IOException {
		JsonToken token = parser.getCurrentToken();
		if (token == null) {
			token = parser.nextToken();
		}
		if (token == JsonToken.START_OBJECT) {
			token = parser.nextToken();
		}

		CollectionMetadata metadata = new CollectionMetadata();
		boolean reachedContentList = false;
		while (token != null && token != JsonToken.END_OBJECT && !reachedContentList) {
			if (token == JsonToken.FIELD_NAME) {
				String name = parser.getCurrentName();
				if (CONCEPTS_FIELD.equals(name) || MAPPINGS_FIELD.equals(name)) {
					reachedContentList = true;
				} else {
					token = readMetadataField(parser, name, metadata);
				}
			} else {
				parser.skipChildren();
				token = parser.nextToken();
			}
		}

		if (!metadata.foundCollection) {
			return;
		}

		String validationSchema = metadata.collectionValidationSchema != null ? metadata.collectionValidationSchema : metadata.topLevelValidationSchema;
		if (!OpenConceptLabConstants.OPEN_MRS_VALIDATION_SCHEMA.equalsIgnoreCase(validationSchema)) {
			throw new ImportException("The collection in the exported data is not validated with the OpenMRS validation"
			        + " schema (custom_validation_schema=" + validationSchema + "). Only collections that use the OpenMRS"
			        + " validation schema can be imported.");
		}
	}

	/**
	 * Reads a single metadata field of the top-level export object, recording the collection
	 * flag and any validation schema declaration, and returns the token after the field's value.
	 */
	private JsonToken readMetadataField(JsonParser parser, String name, CollectionMetadata metadata) throws IOException {
		JsonToken token = parser.nextToken();
		if (COLLECTION_FIELD.equals(name)) {
			metadata.foundCollection = true;
			if (token == JsonToken.START_OBJECT) {
				metadata.collectionValidationSchema = readStringField(parser, CUSTOM_VALIDATION_SCHEMA_FIELD);
			} else {
				parser.skipChildren();
			}
		} else if (CUSTOM_VALIDATION_SCHEMA_FIELD.equals(name)) {
			if (token == JsonToken.VALUE_STRING) {
				metadata.topLevelValidationSchema = parser.getText();
			} else {
				parser.skipChildren();
			}
		} else {
			parser.skipChildren();
		}
		return parser.nextToken();
	}

	/**
	 * Holds the facts gathered from the leading metadata fields of an export.
	 */
	private static class CollectionMetadata {
		boolean foundCollection;
		String topLevelValidationSchema;
		String collectionValidationSchema;
	}

	/**
	 * Scans a JSON object for a string field, returning its value or null when the field is absent
	 * or not a string. The parser must be at the START_OBJECT of the object to scan and is left at
	 * its END_OBJECT.
	 */
	private String readStringField(JsonParser parser, String fieldName) throws IOException {
		String value = null;
		JsonToken token = parser.nextToken();
		while (token != null && token != JsonToken.END_OBJECT) {
			if (token == JsonToken.FIELD_NAME && fieldName.equals(parser.getCurrentName())) {
				token = parser.nextToken();
				if (token == JsonToken.VALUE_STRING) {
					value = parser.getText();
				}
			}
			if (token != null && token != JsonToken.FIELD_NAME) {
				parser.skipChildren();
			}
			token = parser.nextToken();
		}
		return value;
	}

	private JsonToken advanceToListOf(String field, String stopAtField, JsonParser parser) throws IOException {
		JsonToken token = parser.getCurrentToken();
		if (token == null) {
			token = parser.nextToken();
		}
		// Caller ensures we start at the outer START_OBJECT; advance past it.
		if (token == JsonToken.START_OBJECT) {
			token = parser.nextToken();
		}

		while (token != null && token != JsonToken.END_OBJECT) {
			if (token != JsonToken.FIELD_NAME) {
				// Defensive: unexpected token inside an object (e.g. END_ARRAY from a prior array consumption).
				// Skip and continue scanning for the target field.
				parser.skipChildren();
				token = parser.nextToken();
				continue;
			}

			String name = parser.getCurrentName();
			if (field.equals(name)) {
				token = parser.nextToken();
				if (token != JsonToken.START_ARRAY) {
					throw new ImportException(field + " must be a list");
				}
				return token;
			}
			if (stopAtField != null && stopAtField.equals(name)) {
				return token;
			}

			// Skip this field's value (scalar, array, or nested object).
			parser.nextToken();
			parser.skipChildren();
			token = parser.nextToken();
		}
		log.info("Field '{}' not found in JSON object (stopAtField={})", field, stopAtField);
		return null;
	}

	public ImportProgress getImportProgress(String uuid){
		ImportProgress updateProgress = new ImportProgress();

		Import oclImport;
		if (StringUtils.isNotBlank(uuid)) {
			oclImport = importService.getImport(uuid);
		} else {
			oclImport = importService.getLastImport();
		}

		if (oclImport.getLocalDateStopped() == null) {
			long time = (new Date().getTime() - oclImport.getLocalDateStarted().getTime()) / 1000;
			updateProgress.setTime(time);

			if (!isDownloaded()) {
				double totalBytesToDownload = getTotalBytesToDownload();
				double progress;
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
					progress += ((double) time / (time + 100) * 70.0);
				} else {
					progress += ((double) getBytesProcessed() / getTotalBytesToProcess() * 70.0);
				}

				updateProgress.setProgress((int) progress);
			} else {
				updateProgress.setProgress(100);
			}
		} else {
			updateProgress.setProgress(100);
			updateProgress.setTime((oclImport.getLocalDateStopped().getTime() - oclImport.getLocalDateStarted().getTime()) / 1000);
		}

		return updateProgress;
	}

}
