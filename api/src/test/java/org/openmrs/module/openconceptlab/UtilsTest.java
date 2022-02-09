package org.openmrs.module.openconceptlab;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.openmrs.module.openconceptlab.Utils.normalizeConceptSourceName;
import static org.openmrs.module.openconceptlab.Utils.version5Uuid;

import org.junit.Test;

public class UtilsTest {

	@Test
	public void version5Uuid_shouldGenerateExpectedUuid() {
		assertThat(version5Uuid("openmrs").toString(), equalTo("df8436c0-05e2-5d9f-8fd7-40e0f3e9efac"));
		assertThat(version5Uuid("OpenMRS").toString(), equalTo("843d680c-2943-5275-8006-3a0dea1ed08c"));
		assertThat(version5Uuid("Write Code, Save Lives").toString(), equalTo("330fd6a6-43ad-5a84-b406-6006c0869809"));
		assertThat(version5Uuid("Medical Record System").toString(), equalTo("be709f07-95e0-5856-8408-684f82d6d894"));
	}

	@Test
	public void normalizeConceptSourceName_shouldNotConsiderSpacesUnderscoresOrDashes() {
		assertThat(normalizeConceptSourceName("ICD-10"), equalTo(normalizeConceptSourceName("ICD 10")));
		assertThat(normalizeConceptSourceName("ICD-10"), equalTo(normalizeConceptSourceName("ICD10")));
		assertThat(normalizeConceptSourceName("SNOMED CT"), equalTo(normalizeConceptSourceName("SNOMED-CT")));
		assertThat(normalizeConceptSourceName("snomed_ct"), equalTo(normalizeConceptSourceName("SNOMED-CT")));
		assertThat(normalizeConceptSourceName("KenyaEMR"), not(equalTo(normalizeConceptSourceName("KenyaEMR+"))));
	}
}
