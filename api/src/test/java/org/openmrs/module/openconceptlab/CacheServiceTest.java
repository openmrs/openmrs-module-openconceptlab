/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptService;
import org.openmrs.module.openconceptlab.client.OclConcept;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CacheServiceTest extends MockTest {

	@Mock
	ConceptService conceptService;

	@Mock
	ImportService importService;

	static final ConceptSource ICD_10 = conceptSource("ICD-10");
	static final ConceptSource SNOMED_DASH_CT = conceptSource("SNOMED-CT");
	static final ConceptSource SNOMED_SPACE_CT = conceptSource("Snomed CT");

	public List<ConceptSource> getTestConceptSources() {
		List<ConceptSource> l = new ArrayList<>();
		l.add(ICD_10);
		l.add(SNOMED_DASH_CT);
		l.add(SNOMED_SPACE_CT);
		return l;
	}

	public static ConceptSource conceptSource(String name) {
		ConceptSource cs = new ConceptSource();
		cs.setName(name);
		return cs;
	}

	@Before
	public void setupMocks() {
		when(conceptService.getAllConceptSources(true)).thenReturn(getTestConceptSources());
	}

	@Test
	public void cacheServiceShouldReturnConceptSourcesWithFuzzyMatching() {
		CacheService cacheService = new CacheService(conceptService, null);
		assertThat(conceptService.getAllConceptSources(true).size(), is(3));
		assertThat(cacheService.getConceptSourceByName("ICD 10"), is(ICD_10));
		assertThat(cacheService.getConceptSourceByName("ICD_10"), is(ICD_10));
		assertThat(cacheService.getConceptSourceByName("icd 10"), is(ICD_10));
	}

	@Test
	public void cacheServiceShouldReturnConceptSourcesWithExactCaseInsensitiveMatching() {
		CacheService cacheService = new CacheService(conceptService, null);
		assertThat(conceptService.getAllConceptSources(true).size(), is(3));
		assertThat(cacheService.getConceptSourceByName("ICD-10"), is(ICD_10));
		assertThat(cacheService.getConceptSourceByName("SNOMED-CT"), is(SNOMED_DASH_CT));
		assertThat(cacheService.getConceptSourceByName("Snomed CT"), is(SNOMED_SPACE_CT));
		assertThat(cacheService.getConceptSourceByName("icd-10"), is(ICD_10));
		assertThat(cacheService.getConceptSourceByName("snomed-CT"), is(SNOMED_DASH_CT));
		assertThat(cacheService.getConceptSourceByName("SNOMED CT"), is(SNOMED_SPACE_CT));
	}

	@Test
	public void cacheServiceShouldThrowAnExceptionIfNoExactConceptSourcesAndMultipleFuzzyMatchesAreFound() {
		CacheService cacheService = new CacheService(conceptService, null);
		Exception actualException = null;
		try {
			cacheService.getConceptSourceByName("snomedct");
		}
		catch (Exception e) {
			actualException = e;
		}
		assertThat(actualException, notNullValue());
	}

	@Test
	public void getLastSuccessfulItemByUrl_shouldCacheResultsAcrossCalls() {
		CacheService cacheService = new CacheService(conceptService, null);
		String url = "/orgs/OCL/sources/Diagnoses/concepts/123/";
		Item item = createItem(url, "v1", ItemState.ADDED);
		when(importService.getLastSuccessfulItemByUrl(url, cacheService)).thenReturn(item);

		Item first = cacheService.getLastSuccessfulItemByUrl(url, importService);
		Item second = cacheService.getLastSuccessfulItemByUrl(url, importService);

		assertThat(first, is(item));
		assertThat(second, is(item));
		verify(importService, times(1)).getLastSuccessfulItemByUrl(url, cacheService);
	}

	@Test
	public void getLastSuccessfulItemByUrl_shouldCacheNullResults() {
		CacheService cacheService = new CacheService(conceptService, null);
		String url = "/orgs/OCL/sources/Diagnoses/concepts/999/";
		when(importService.getLastSuccessfulItemByUrl(url, cacheService)).thenReturn(null);

		Item first = cacheService.getLastSuccessfulItemByUrl(url, importService);
		Item second = cacheService.getLastSuccessfulItemByUrl(url, importService);

		assertThat(first, nullValue());
		assertThat(second, nullValue());
		verify(importService, times(1)).getLastSuccessfulItemByUrl(url, cacheService);
	}

	@Test
	public void getLastSuccessfulItemByUrl_shouldSkipDbWhenSkipDbItemLookupsIsTrue() {
		CacheService cacheService = new CacheService(conceptService, null);
		cacheService.setSkipDbItemLookups(true);
		String url = "/orgs/OCL/sources/Diagnoses/concepts/123/";

		Item result = cacheService.getLastSuccessfulItemByUrl(url, importService);

		assertThat(result, nullValue());
		verify(importService, never()).getLastSuccessfulItemByUrl(url, cacheService);
	}

	@Test
	public void getLastSuccessfulItemByUrl_shouldReturnCachedItemEvenWhenSkipDbItemLookupsIsTrue() {
		CacheService cacheService = new CacheService(conceptService, null);
		String url = "/orgs/OCL/sources/Diagnoses/concepts/123/";
		Item item = createItem(url, "v1", ItemState.ADDED);
		cacheService.cacheItem(item);
		cacheService.setSkipDbItemLookups(true);

		Item result = cacheService.getLastSuccessfulItemByUrl(url, importService);

		assertThat(result, is(item));
		verify(importService, never()).getLastSuccessfulItemByUrl(url, cacheService);
	}

	@Test
	public void cacheItem_shouldNotCacheErrorItems() {
		CacheService cacheService = new CacheService(conceptService, null);
		String url = "/orgs/OCL/sources/Diagnoses/concepts/456/";
		Item errorItem = createItem(url, "v1", ItemState.ERROR);
		cacheService.cacheItem(errorItem);

		// URL not in cache, so should fall through to DB lookup
		when(importService.getLastSuccessfulItemByUrl(url, cacheService)).thenReturn(null);
		Item result = cacheService.getLastSuccessfulItemByUrl(url, importService);

		assertThat(result, nullValue());
		verify(importService, times(1)).getLastSuccessfulItemByUrl(url, cacheService);
	}

	@Test
	public void cacheItem_shouldPersistAcrossClearCacheCalls() {
		CacheService cacheService = new CacheService(conceptService, null);
		String url = "/orgs/OCL/sources/Diagnoses/concepts/789/";
		Item item = createItem(url, "v1", ItemState.ADDED);
		cacheService.cacheItem(item);

		cacheService.clearCache();

		Item result = cacheService.getLastSuccessfulItemByUrl(url, importService);
		assertThat(result, is(item));
		verify(importService, never()).getLastSuccessfulItemByUrl(url, cacheService);
	}

	@Test
	public void getConceptMapByUuid_shouldCacheResults() {
		CacheService cacheService = new CacheService(conceptService, null);
		String uuid = "map-uuid-123";
		ConceptMap map = new ConceptMap();
		when(importService.getConceptMapByUuid(uuid)).thenReturn(map);

		ConceptMap first = cacheService.getConceptMapByUuid(uuid, importService);
		ConceptMap second = cacheService.getConceptMapByUuid(uuid, importService);

		assertThat(first, is(map));
		assertThat(second, is(map));
		verify(importService, times(1)).getConceptMapByUuid(uuid);
	}

	@Test
	public void getConceptMapByUuid_shouldStillQueryDbWhenSkipDbItemLookupsIsTrue() {
		CacheService cacheService = new CacheService(conceptService, null);
		cacheService.setSkipDbItemLookups(true);
		String uuid = "map-uuid-456";
		ConceptMap map = new ConceptMap();
		when(importService.getConceptMapByUuid(uuid)).thenReturn(map);

		ConceptMap result = cacheService.getConceptMapByUuid(uuid, importService);

		assertThat(result, is(map));
		verify(importService, times(1)).getConceptMapByUuid(uuid);
	}

	private Item createItem(String url, String versionUrl, ItemState state) {
		OclConcept concept = new OclConcept();
		concept.setUrl(url);
		concept.setVersionUrl(versionUrl);
		concept.setExternalId("ext-" + url.hashCode());
		return new Item(null, concept, state);
	}
}
