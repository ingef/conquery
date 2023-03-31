package com.bakdata.conquery.io.external.form;

import static com.bakdata.conquery.io.external.form.ExternalFormMixin.Serializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.bakdata.conquery.apiv1.forms.ExternalForm;
import com.bakdata.conquery.models.config.FormBackendConfig;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.extern.slf4j.Slf4j;

/**
 * Mixin class, which prepare an external form for an external form backend.
 * It takes care of:
 * <ul>
 *     <li>Substituting the type id from the conquery type id override (see {@link FormBackendConfig}) back to the id the form backend expects</li>
 *     <li>Removing the <pre>values</pre> member, which holds only frontend specific data</li>
 * </ul>
 */
@JsonSerialize(using = Serializer.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class ExternalFormMixin {

	@Slf4j
	public static class Serializer extends StdSerializer<ExternalForm> {

		protected Serializer() {
			super(ExternalForm.class);
		}

		@Override
		public void serialize(ExternalForm value, JsonGenerator gen, SerializerProvider provider) throws IOException {

			gen.writeStartObject();

			// Write original form backend subtype
			gen.writeStringField("type", value.getSubType());

			final Iterator<Map.Entry<String, JsonNode>> fields = value.getNode().fields();
			while (fields.hasNext()) {
				final Map.Entry<String, JsonNode> next = fields.next();
				if (next.getKey().equals("values")) {
					// Skip this node for serialization
					continue;
				}
				gen.writeObjectField(next.getKey(), next.getValue());
			}

			gen.writeEndObject();
		}
	}
}
