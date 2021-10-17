package org.openmrs.module.openconceptlab.client.Deserializer;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.openmrs.module.openconceptlab.client.OclConcept;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StringBooleanDeserializerTest {
	@Test
	public void stringBooleanDeserializer_shouldDeserialiseBooleanString()
			throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		OclConcept oclConcept = mapper.readValue("{"
				+ "\"id\":467179,"
				+ "\"public access\":\"Edit\","
				+ "\"created at\":\"2021-10-01T13:50:28.293664+00:00\","
				+ "\"updated at\":\"2021-10-01T13:50:28.302355+00:00\","
				+ "\"created_by_id\":789,"
				+ "\"updated_by_id\":789,"
				+ "\"is active\":\"True\","
				+ "\"extras\": {"
				+ "      \"precise\": \"TRUE\""
				+ "},"
				+ "\"uri\": \"/orgs/MSFOCP/sources/OCL-CSV-Import-Test/concepts/OCLCSV07/467179/\","
				+ "\"version\": 467179,"
				+ "\"released\": \"True\","
				+ "\"retired\": \"False\","
				+ "\"is latest version\": \"False\","
				+ "\"name\": \"\","
				+ "\"full name\": \"\","
				+ "\"default locale\": \"en\","
				+ "\"supported locales\": \"\","
				+ "\"website\": \"\","
				+ "\"description\": \"\""
				+ "}",OclConcept.class);

		assertThat(oclConcept.getExtras().getPrecise(), is(true));
		assertThat(oclConcept.isRetired(), is(false));
	}
}
