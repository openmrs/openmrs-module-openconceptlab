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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.openconceptlab.CacheService;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.TestResources;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportServiceImpl;
import org.openmrs.module.openconceptlab.client.OclClient;
import org.openmrs.module.openconceptlab.client.OclClient.OclResponse;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.ValidationType;
import org.openmrs.module.openconceptlab.client.OclMapping;
import org.openmrs.test.BaseContextMockTest;

public class ImporterTest extends BaseContextMockTest {

	@Mock
	OclClient oclClient;

	@Mock
	ContextDAO contextDAO;

	@Mock
	ImportServiceImpl importService;

	@Mock
	CacheService CacheService;

	@Mock
	Saver saver;

    @Mock
    Subscription subscription;

	Import lastSuccessfulImport;

	@InjectMocks
	Importer importer;

	@Before
	public void before() {
		TestResources.setupDaemonToken();
	}

	/**
	 * @see Importer#run()
	 * @verifies start first anImport with response date
	 */
	@Test
	public void runUpdate_shouldStartFirstUpdateWithResponseDate() throws Exception {
		Subscription subscription = new Subscription();
		subscription.setUrl("http://some.com/url");
		when(importService.getSubscription()).thenReturn(subscription);

		Date updatedTo = new Date();
		OclResponse oclResponse = new OclClient.OclResponse(IOUtils.toInputStream("{}"), 0, updatedTo);
		when(importService.getLastImport()).thenReturn(null);
		when(oclClient.fetchOclConcepts(subscription.getUrl(), subscription.getToken())).thenReturn(oclResponse);

		importer.run();

		verify(importService).updateOclDateStarted(any(Import.class), Mockito.eq(updatedTo));
	}

	/**
	 * @see Importer#run()
	 * @verifies start further RELEASE update
	 */
	@Test
	public void runUpdate_shouldStartUpdateIfNewRelease() throws Exception {

        final String release1name = "1.0";
        final String release2name = "1.1";

        subscription.setUrl("http://some.com/url");
		when(importService.getSubscription()).thenReturn(subscription);
		when(subscription.isSubscribedToSnapshot()).thenReturn(false);

		Import lastImport = new Import();
		Date updatedSince = new Date();
		lastImport.setOclDateStarted(updatedSince);
		lastImport.setReleaseVersion(release1name);

		when(importService.getLastSuccessfulSubscriptionImport()).thenReturn(lastImport);

        OclResponse oclResponse = new OclClient.OclResponse(IOUtils.toInputStream("{}"), 0, new Date());

        when(oclClient.fetchLatestOclReleaseVersion(subscription.getUrl(), subscription.getToken())).thenReturn(release2name);
        when(oclClient.fetchOclConcepts(subscription.getUrl(), subscription.getToken(), lastImport.getReleaseVersion())).thenReturn(oclResponse);

        importer.run();
    }

	/**
	 * @see Importer#run()
	 * @verifies start further SNAPSHOT import with updated since
	 */
	@Test
	public void runUpdate_shouldStartNextUpdateWithUpdatedSince() throws Exception {
		subscription.setUrl("http://some.com/url");
		when(importService.getSubscription()).thenReturn(subscription);
		when(subscription.isSubscribedToSnapshot()).thenReturn(true);

		Import lastUpdate = new Import();
		Date updatedSince = new Date();
		lastUpdate.setOclDateStarted(updatedSince);
		when(importService.getLastSuccessfulSubscriptionImport()).thenReturn(lastUpdate);

		Date updatedTo = new Date();
		OclResponse oclResponse = new OclClient.OclResponse(IOUtils.toInputStream("{}"), 0, updatedTo);
		when(oclClient.fetchSnapshotUpdates(subscription.getUrl(), subscription.getToken(), lastUpdate.getOclDateStarted()))
		        .thenReturn(oclResponse);

		importer.run();

		verify(importService).updateOclDateStarted(any(Import.class), Mockito.eq(updatedTo));
	}

	/**
	 * @see Importer#run()
	 * @verifies create item for each concept and mapping
	 */
	@Test
	public void runUpdate_shouldCreateItemForEachConceptAndMapping() throws Exception {
		subscription.setUrl("http://some.com/url");
		when(importService.getSubscription()).thenReturn(subscription);
		when(subscription.isSubscribedToSnapshot()).thenReturn(true);

		Import lastUpdate = new Import();
		Date updatedSince = new Date();
		lastUpdate.setOclDateStarted(updatedSince);

		when(importService.getLastSuccessfulSubscriptionImport()).thenReturn(lastUpdate);

		Import anImport = new Import();
		when(importService.getImport(nullable(Long.class))).thenReturn(anImport);

		Date updatedTo = new Date();
		OclResponse oclResponse = OclClient.unzipResponse(TestResources.getSimpleResponseAsStream(), updatedTo);

		when(oclClient.fetchSnapshotUpdates(subscription.getUrl(), subscription.getToken(), lastUpdate.getOclDateStarted()))
		        .thenReturn(oclResponse);

		doAnswer(new Answer<Item>() {

			@Override
			public Item answer(InvocationOnMock invocation) throws Throwable {
				Import update = (Import) invocation.getArguments()[1];
				OclConcept oclConcept = (OclConcept) invocation.getArguments()[2];
				return new Item(update, oclConcept, ItemState.ADDED);
			}
		}).when(saver).saveConcept(any(CacheService.class), any(Import.class), any(OclConcept.class), any(ValidationType.class));

		doAnswer(new Answer<Item>() {

			@Override
			public Item answer(InvocationOnMock invocation) throws Throwable {
				Import update = (Import) invocation.getArguments()[1];
				OclMapping oclMapping = (OclMapping) invocation.getArguments()[2];
				return new Item(update, oclMapping, ItemState.ADDED);
			}

		}).when(saver).saveMapping(any(CacheService.class), any(Import.class), any(OclMapping.class));

		importer.run();

		//concepts
		verify(importService).saveItems(
		    argThat(hasItems(hasUuid("1001AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"),
		        hasUuid("1002AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"), hasUuid("1003AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))));

		//mappings
		verify(importService).saveItems(
		    argThat(hasItems(hasUuid("697bf112-a7ca-3ae3-af4f-8b46e3af7f10"),
		        hasUuid("def16c32-0635-3afd-8a56-a080830e2bff"), hasUuid("b705416c-ad04-356f-9d43-8945ee382722"))));
	}

	/**
	 * @see Importer#advanceToListOf(String, String, JsonParser)
	 * @verifies find concepts array in minimal JSON with no metadata wrapper
	 */
	@Test
	public void advanceToListOf_shouldFindConceptsArrayInMinimalJson() throws Exception {
		String json = "{\"concepts\":[{\"id\":\"1\"}],\"mappings\":[]}";
		JsonParser parser = createParser(json);

		JsonToken result = invokeAdvanceToListOf("concepts", "mappings", parser);

		assertEquals(JsonToken.START_ARRAY, result);
		// Parser is at START_ARRAY; next token is START_OBJECT of first concept
		assertEquals(JsonToken.START_OBJECT, parser.nextToken());
	}

	/**
	 * @see Importer#advanceToListOf(String, String, JsonParser)
	 * @verifies skip nested objects correctly when concepts comes first
	 */
	@Test
	public void advanceToListOf_shouldSkipNestedObjectsBeforeTargetField() throws Exception {
		String json = "{\"extras\":{\"nested\":{\"deep\":true}},\"concepts\":[{\"id\":\"1\"}],\"mappings\":[]}";
		JsonParser parser = createParser(json);

		JsonToken result = invokeAdvanceToListOf("concepts", "mappings", parser);

		assertEquals(JsonToken.START_ARRAY, result);
	}

	/**
	 * @see Importer#advanceToListOf(String, String, JsonParser)
	 * @verifies find mappings array after concepts
	 */
	@Test
	public void advanceToListOf_shouldFindMappingsArrayAfterConcepts() throws Exception {
		String json = "{\"concepts\":[{\"id\":\"1\",\"names\":[{\"name\":\"x\",\"locale\":\"en\"}]}],\"mappings\":[{\"id\":\"m1\"}]}";
		JsonParser parser = createParser(json);

		JsonToken result = invokeAdvanceToListOf("concepts", "mappings", parser);
		assertEquals(JsonToken.START_ARRAY, result);

		// Skip past the concepts array
		parser.skipChildren();
		// Now advance to mappings
		result = invokeAdvanceToListOf("mappings", null, parser);
		assertEquals(JsonToken.START_ARRAY, result);
	}

	/**
	 * @see Importer#advanceToListOf(String, String, JsonParser)
	 * @verifies stop at stopAtField when target field is missing
	 */
	@Test
	public void advanceToListOf_shouldStopAtStopAtFieldWhenTargetIsMissing() throws Exception {
		String json = "{\"mappings\":[]}";
		JsonParser parser = createParser(json);

		JsonToken result = invokeAdvanceToListOf("concepts", "mappings", parser);

		// Should return FIELD_NAME token for "mappings" (stopAtField hit)
		assertNotNull(result);
		assertEquals(JsonToken.FIELD_NAME, result);
		assertEquals("mappings", parser.getCurrentName());
	}

	/**
	 * @see Importer#advanceToListOf(String, String, JsonParser)
	 * @verifies return null when field not found and no stopAtField
	 */
	@Test
	public void advanceToListOf_shouldReturnNullWhenFieldNotFound() throws Exception {
		String json = "{\"other\":123}";
		JsonParser parser = createParser(json);

		JsonToken result = invokeAdvanceToListOf("concepts", null, parser);

		assertNull(result);
	}

	/**
	 * @see Importer#advanceToListOf(String, String, JsonParser)
	 * @verifies throw ImportException if target field is not an array
	 */
	@Test
	public void advanceToListOf_shouldThrowImportExceptionIfTargetIsNotArray() throws Exception {
		String json = "{\"concepts\":\"not_an_array\"}";
		JsonParser parser = createParser(json);

		try {
			invokeAdvanceToListOf("concepts", null, parser);
		} catch (Exception e) {
			// Reflection wraps in InvocationTargetException
			assertTrue(e.getCause() instanceof ImportException);
			return;
		}
		throw new AssertionError("Expected ImportException");
	}

	/**
	 * @see Importer#advanceToListOf(String, String, JsonParser)
	 * @verifies handle empty target array
	 */
	@Test
	public void advanceToListOf_shouldHandleEmptyTargetArray() throws Exception {
		String json = "{\"concepts\":[],\"mappings\":[]}";
		JsonParser parser = createParser(json);

		JsonToken result = invokeAdvanceToListOf("concepts", "mappings", parser);

		assertEquals(JsonToken.START_ARRAY, result);
		// Next token should be END_ARRAY (empty array)
		assertEquals(JsonToken.END_ARRAY, parser.nextToken());
	}

	/**
	 * @see Importer#advanceToListOf(String, String, JsonParser)
	 * @verifies handle JSON with deeply nested objects in concept arrays
	 */
	@Test
	public void advanceToListOf_shouldHandleDeeplyNestedObjectsInConcepts() throws Exception {
		String json = "{\"concepts\":[{\"id\":\"1\",\"names\":[{\"name\":\"x\",\"locale\":\"en\",\"extras\":{\"nested\":{\"deep\":true}}}],\"extras\":{\"more\":{\"nesting\":{\"here\":1}}}}],\"mappings\":[]}";
		JsonParser parser = createParser(json);

		// First: find concepts
		JsonToken result = invokeAdvanceToListOf("concepts", "mappings", parser);
		assertEquals(JsonToken.START_ARRAY, result);

		// Skip past the concepts array
		parser.skipChildren();

		// Then: find mappings
		result = invokeAdvanceToListOf("mappings", null, parser);
		assertEquals(JsonToken.START_ARRAY, result);
	}

	/**
	 * @see Importer#advanceToListOf(String, String, JsonParser)
	 * @verifies handle the exact minimal JSON that previously broke the importer
	 */
	@Test
	public void advanceToListOf_shouldHandleMinimalJsonWithoutExtras() throws Exception {
		// This is the exact JSON shape from the bug report
		String json = "{\"concepts\":[{\"external_id\":\"\",\"url\":\"/orgs/Test/sources/Test/concepts/1/\",\"version_url\":\"/orgs/Test/sources/Test/concepts/1/v1/\",\"concept_class\":\"Misc\",\"datatype\":\"Text\",\"retired\":false,\"names\":[{\"name\":\"x\",\"locale\":\"en\",\"name_type\":\"FULLY_SPECIFIED\",\"locale_preferred\":true}],\"descriptions\":[]}],\"mappings\":[]}";
		JsonParser parser = createParser(json);

		JsonToken result = invokeAdvanceToListOf("concepts", "mappings", parser);
		assertEquals(JsonToken.START_ARRAY, result);

		// Verify parser is positioned at START_ARRAY, and we can read through the concept
		parser.nextToken(); // advance to first element inside array
		assertEquals(JsonToken.START_OBJECT, parser.getCurrentToken());
		parser.nextToken(); // first field inside the concept
		assertEquals("external_id", parser.getCurrentName());
	}

	/**
	 * @see Importer#advanceToListOf(String, String, JsonParser)
	 * @verifies stop at stopAtField even when target field exists later in the JSON
	 */
	@Test
	public void advanceToListOf_shouldStopAtStopAtFieldEvenWhenTargetFieldExistsLater() throws Exception {
		// mappings appears before concepts — stopAtField should prevent finding concepts
		String json = "{\"mappings\":[{\"id\":\"m1\"}],\"concepts\":[{\"id\":\"1\"}]}";
		JsonParser parser = createParser(json);

		JsonToken result = invokeAdvanceToListOf("concepts", "mappings", parser);

		// Should stop at "mappings" without continuing to find "concepts"
		assertNotNull(result);
		assertEquals(JsonToken.FIELD_NAME, result);
		assertEquals("mappings", parser.getCurrentName());
	}

	/**
	 * @see Importer#advanceToListOf(String, String, JsonParser)
	 * @verifies work correctly when parser is already positioned at FIELD_NAME (past START_OBJECT)
	 */
	@Test
	public void advanceToListOf_shouldWorkWhenParserAlreadyAtFieldName() throws Exception {
		String json = "{\"concepts\":[{\"id\":\"1\"}],\"mappings\":[]}";
		JsonParser parser = new ObjectMapper().getJsonFactory().createJsonParser(new ByteArrayInputStream(json.getBytes("UTF-8")));
		parser.nextToken(); // START_OBJECT
		parser.nextToken(); // FIELD_NAME "concepts" — simulating caller already advancing past START_OBJECT

		JsonToken result = invokeAdvanceToListOf("concepts", "mappings", parser);

		assertEquals(JsonToken.START_ARRAY, result);
	}

	/**
	 * @see Importer#run()
	 * @verifies import mappings even when concepts array is absent from JSON
	 */
	@Test
	public void runUpdate_shouldImportMappingsWhenConceptsAreAbsent() throws Exception {
		subscription.setUrl("http://some.com/url");
		when(importService.getSubscription()).thenReturn(subscription);
		when(subscription.isSubscribedToSnapshot()).thenReturn(true);

		Import lastUpdate = new Import();
		Date updatedSince = new Date();
		lastUpdate.setOclDateStarted(updatedSince);

		when(importService.getLastSuccessfulSubscriptionImport()).thenReturn(lastUpdate);

		Import anImport = new Import();
		when(importService.getImport(nullable(Long.class))).thenReturn(anImport);

		// Mappings-only JSON — no concepts array at all
		String mappingsOnlyJson = "{\"mappings\":[{\"external_id\":\"m1\",\"map_type\":\"Same As\",\"from_concept_url\":\"/orgs/Test/sources/Test/concepts/1/\",\"to_concept_url\":\"/orgs/Test/sources/Test/concepts/2/\",\"url\":\"/orgs/Test/sources/Test/mappings/m1/\"}]}";
		OclResponse oclResponse = new OclClient.OclResponse(
				IOUtils.toInputStream(mappingsOnlyJson), mappingsOnlyJson.length(), new Date());

		when(oclClient.fetchSnapshotUpdates(subscription.getUrl(), subscription.getToken(), lastUpdate.getOclDateStarted()))
				.thenReturn(oclResponse);

		doAnswer(new Answer<Item>() {
			@Override
			public Item answer(InvocationOnMock invocation) throws Throwable {
				Import update = (Import) invocation.getArguments()[1];
				OclMapping oclMapping = (OclMapping) invocation.getArguments()[2];
				return new Item(update, oclMapping, ItemState.ADDED);
			}
		}).when(saver).saveMapping(any(CacheService.class), any(Import.class), any(OclMapping.class));

		importer.run();

		// Verify mappings were saved (saveItems called with at least one mapping item)
		verify(importService).saveItems(
				argThat(hasItems(hasUrl("/orgs/Test/sources/Test/mappings/m1/"))));
	}

	/**
	 * @see Importer#run()
	 * @verifies import mappings when mappings appears before concepts in JSON
	 */
	@Test
	public void runUpdate_shouldImportMappingsWhenMappingsAppearBeforeConcepts() throws Exception {
		subscription.setUrl("http://some.com/url");
		when(importService.getSubscription()).thenReturn(subscription);
		when(subscription.isSubscribedToSnapshot()).thenReturn(true);

		Import lastUpdate = new Import();
		Date updatedSince = new Date();
		lastUpdate.setOclDateStarted(updatedSince);

		when(importService.getLastSuccessfulSubscriptionImport()).thenReturn(lastUpdate);

		Import anImport = new Import();
		when(importService.getImport(nullable(Long.class))).thenReturn(anImport);

		// mappings before concepts — the stopAtField will cause advanceToListOf to return FIELD_NAME
		String mappingsFirstJson = "{\"mappings\":[{\"external_id\":\"m1\",\"map_type\":\"Same As\",\"from_concept_url\":\"/orgs/Test/sources/Test/concepts/1/\",\"to_concept_url\":\"/orgs/Test/sources/Test/concepts/2/\",\"url\":\"/orgs/Test/sources/Test/mappings/m1/\"}],\"concepts\":[{\"id\":\"1\"}]}";
		OclResponse oclResponse = new OclClient.OclResponse(
				IOUtils.toInputStream(mappingsFirstJson), mappingsFirstJson.length(), new Date());

		when(oclClient.fetchSnapshotUpdates(subscription.getUrl(), subscription.getToken(), lastUpdate.getOclDateStarted()))
				.thenReturn(oclResponse);

		doAnswer(new Answer<Item>() {
			@Override
			public Item answer(InvocationOnMock invocation) throws Throwable {
				Import update = (Import) invocation.getArguments()[1];
				OclMapping oclMapping = (OclMapping) invocation.getArguments()[2];
				return new Item(update, oclMapping, ItemState.ADDED);
			}
		}).when(saver).saveMapping(any(CacheService.class), any(Import.class), any(OclMapping.class));

		importer.run();

		// Verify mappings were saved even though mappings appeared before concepts
		verify(importService).saveItems(
				argThat(hasItems(hasUrl("/orgs/Test/sources/Test/mappings/m1/"))));
	}

	/**
	 * @see Importer#verifyCollectionUsesOpenMRSValidationSchema(JsonParser)
	 * @verifies allow a collection export validated with the OpenMRS schema
	 */
	@Test
	public void verifyCollectionSchema_shouldAllowOpenMRSValidatedCollection() throws Exception {
		String json = "{\"type\":\"Collection Version\",\"collection\":{\"id\":\"refapp\",\"custom_validation_schema\":\"OpenMRS\"},\"concepts\":[],\"mappings\":[]}";
		JsonParser parser = createParser(json);

		invokeVerifyCollectionSchema(parser);

		assertEquals(JsonToken.FIELD_NAME, parser.getCurrentToken());
		assertEquals("concepts", parser.getCurrentName());
		assertEquals(JsonToken.START_ARRAY, invokeAdvanceToListOf("concepts", "mappings", parser));
	}

	/**
	 * @see Importer#verifyCollectionUsesOpenMRSValidationSchema(JsonParser)
	 * @verifies treat the OpenMRS schema as case-insensitive
	 */
	@Test
	public void verifyCollectionSchema_shouldAllowOpenMRSSchemaCaseInsensitive() throws Exception {
		String json = "{\"collection\":{\"id\":\"refapp\",\"custom_validation_schema\":\"openmrs\"},\"concepts\":[],\"mappings\":[]}";

		invokeVerifyCollectionSchema(createParser(json));
	}

	/**
	 * @see Importer#verifyCollectionUsesOpenMRSValidationSchema(JsonParser)
	 * @verifies fall back to the top level custom_validation_schema for the current OCL format
	 */
	@Test
	public void verifyCollectionSchema_shouldFallBackToTopLevelSchemaForCurrentFormat() throws Exception {
		String json = "{\"type\":\"Collection Version\",\"custom_validation_schema\":\"OpenMRS\",\"collection\":{\"id\":\"refapp\"},\"concepts\":[],\"mappings\":[]}";

		invokeVerifyCollectionSchema(createParser(json));
	}

	/**
	 * @see Importer#verifyCollectionUsesOpenMRSValidationSchema(JsonParser)
	 * @verifies prefer the nested collection schema over the top level schema
	 */
	@Test
	public void verifyCollectionSchema_shouldPreferNestedSchemaOverTopLevel() throws Exception {
		String json = "{\"custom_validation_schema\":\"OCL\",\"collection\":{\"id\":\"refapp\",\"custom_validation_schema\":\"OpenMRS\"},\"concepts\":[],\"mappings\":[]}";

		invokeVerifyCollectionSchema(createParser(json));
	}

	/**
	 * @see Importer#verifyCollectionUsesOpenMRSValidationSchema(JsonParser)
	 * @verifies allow a source export without a collection object
	 */
	@Test
	public void verifyCollectionSchema_shouldAllowSourceExportWithoutCollection() throws Exception {
		String json = "{\"type\":\"Source\",\"short_code\":\"CIEL\",\"concepts\":[],\"mappings\":[]}";

		invokeVerifyCollectionSchema(createParser(json));
	}

	/**
	 * @see Importer#verifyCollectionUsesOpenMRSValidationSchema(JsonParser)
	 * @verifies reject a collection export that declares a non OpenMRS schema
	 */
	@Test
	public void verifyCollectionSchema_shouldRejectNonOpenMRSSchema() throws Exception {
		String json = "{\"collection\":{\"id\":\"refapp\",\"custom_validation_schema\":\"OCL\"},\"concepts\":[],\"mappings\":[]}";

		expectImportException(() -> invokeVerifyCollectionSchema(createParser(json)));
	}

	/**
	 * @see Importer#verifyCollectionUsesOpenMRSValidationSchema(JsonParser)
	 * @verifies reject a collection export without a custom_validation_schema field
	 */
	@Test
	public void verifyCollectionSchema_shouldRejectCollectionWithoutSchemaField() throws Exception {
		String json = "{\"collection\":{\"id\":\"refapp\"},\"concepts\":[],\"mappings\":[]}";

		expectImportException(() -> invokeVerifyCollectionSchema(createParser(json)));
	}

	/**
	 * @see Importer#run()
	 * @verifies fail the import when the collection schema is not OpenMRS
	 */
	@Test
	public void runUpdate_shouldFailWhenCollectionValidationSchemaIsNotOpenMRS() throws Exception {
		setupSnapshotSubscription();

		String badCollectionJson = "{\"type\":\"Collection Version\",\"collection\":{\"id\":\"test\",\"custom_validation_schema\":\"OCL\"},\"concepts\":[],\"mappings\":[]}";
		OclResponse oclResponse = new OclClient.OclResponse(IOUtils.toInputStream(badCollectionJson),
		        badCollectionJson.length(), new Date());
		when(oclClient.fetchSnapshotUpdates(subscription.getUrl(), subscription.getToken(),
		        lastSuccessfulImport.getOclDateStarted())).thenReturn(oclResponse);

		importer.run();

		verify(importService).failImport(any(Import.class),
		    argThat(containsString("custom_validation_schema=OCL")));
	}

	/**
	 * @see Importer#run()
	 * @verifies fail the import when the collection schema field is missing
	 */
	@Test
	public void runUpdate_shouldFailWhenCollectionValidationSchemaIsMissing() throws Exception {
		setupSnapshotSubscription();

		String missingSchemaJson = "{\"type\":\"Collection Version\",\"collection\":{\"id\":\"test\"},\"concepts\":[],\"mappings\":[]}";
		OclResponse oclResponse = new OclClient.OclResponse(IOUtils.toInputStream(missingSchemaJson),
		        missingSchemaJson.length(), new Date());
		when(oclClient.fetchSnapshotUpdates(subscription.getUrl(), subscription.getToken(),
		        lastSuccessfulImport.getOclDateStarted())).thenReturn(oclResponse);

		importer.run();

		verify(importService).failImport(any(Import.class),
		    argThat(containsString("custom_validation_schema=null")));
	}

	/**
	 * @see Importer#run()
	 * @verifies import an OpenMRS validated collection
	 */
	@Test
	public void runUpdate_shouldImportOpenMRSValidatedCollection() throws Exception {
		setupSnapshotSubscription();

		Date updatedTo = new Date();
		OclResponse oclResponse = OclClient.unzipResponse(
		        TestResources.getResponseAsStream("refapp-collection-response-v1/response.zip"), updatedTo);
		when(oclClient.fetchSnapshotUpdates(subscription.getUrl(), subscription.getToken(),
		        lastSuccessfulImport.getOclDateStarted())).thenReturn(oclResponse);

		doAnswer(new Answer<Item>() {
			@Override
			public Item answer(InvocationOnMock invocation) throws Throwable {
				Import update = (Import) invocation.getArguments()[1];
				OclConcept oclConcept = (OclConcept) invocation.getArguments()[2];
				return new Item(update, oclConcept, ItemState.ADDED);
			}
		}).when(saver).saveConcept(any(CacheService.class), any(Import.class), any(OclConcept.class),
		        any(ValidationType.class));

		importer.run();

		verify(saver).saveConcept(any(CacheService.class), any(Import.class), any(OclConcept.class),
		        any(ValidationType.class));
	}

	private void setupSnapshotSubscription() {
		subscription.setUrl("http://some.com/url");
		when(importService.getSubscription()).thenReturn(subscription);
		when(subscription.isSubscribedToSnapshot()).thenReturn(true);

		lastSuccessfulImport = new Import();
		lastSuccessfulImport.setOclDateStarted(new Date());
		when(importService.getLastSuccessfulSubscriptionImport()).thenReturn(lastSuccessfulImport);

		when(importService.getImport(nullable(Long.class))).thenReturn(new Import());
	}

	private interface ExceptionRunnable {
		void run() throws Exception;
	}

	private void expectImportException(ExceptionRunnable runnable) throws Exception {
		try {
			runnable.run();
			fail("Expected ImportException");
		}
		catch (Exception e) {
			assertTrue("Expected ImportException, got: " + e,
			    e instanceof ImportException || e.getCause() instanceof ImportException);
		}
	}

	private void invokeVerifyCollectionSchema(JsonParser parser) throws Exception {
		Method method = Importer.class.getDeclaredMethod("verifyCollectionUsesOpenMRSValidationSchema", JsonParser.class);
		method.setAccessible(true);
		method.invoke(importer, parser);
	}

	private JsonParser createParser(String json) throws IOException {
		JsonParser parser = new ObjectMapper().getJsonFactory().createJsonParser(new ByteArrayInputStream(json.getBytes("UTF-8")));
		assertEquals(JsonToken.START_OBJECT, parser.nextToken());
		return parser;
	}

	private JsonToken invokeAdvanceToListOf(String field, String stopAtField, JsonParser parser) throws Exception {
		Method method = Importer.class.getDeclaredMethod("advanceToListOf", String.class, String.class, JsonParser.class);
		method.setAccessible(true);
		return (JsonToken) method.invoke(importer, field, stopAtField, parser);
	}

	public Matcher<Item> hasUuid(String uuid) {
		return new FeatureMatcher<Item, String>(
		                                        is(uuid), "uuid", "uuid") {

			@Override
			protected String featureValueOf(Item actual) {
				return actual.getUuid();
			}
		};
	}

	public Matcher<Item> hasUrl(String url) {
		return new FeatureMatcher<Item, String>(is(url), "url", "url") {

			@Override
			protected String featureValueOf(Item actual) {
				return actual.getUrl();
			}
		};
	}
}
