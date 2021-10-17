package org.openmrs.module.openconceptlab.client.Deserializer;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;

public class StringBooleanDeserializer extends JsonDeserializer<Boolean> {

	@Override
	public Boolean deserialize(JsonParser parser, DeserializationContext context)
			throws IOException {
		return Boolean.parseBoolean(parser.getText());
	}
}
