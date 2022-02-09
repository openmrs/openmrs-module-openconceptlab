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
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptService;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class CacheServiceTest extends MockTest {

	@Mock
	ConceptService conceptService;

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
}
