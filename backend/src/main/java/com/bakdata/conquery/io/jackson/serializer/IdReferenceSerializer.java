package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class IdReferenceSerializer extends StdSerializer<Identifiable> implements ContextualSerializer {

	private static final long serialVersionUID = 1L;

	private final boolean useToString;

	public IdReferenceSerializer() {
		this(true);
	}

	public IdReferenceSerializer(boolean useToString) {
		super(Identifiable.class);
		this.useToString = useToString;
	}

	@Override
	public void serializeWithType(Identifiable value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
		serialize(value, gen, serializers);
	}

	@Override
	public void serialize(Identifiable value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		if (useToString) {
			gen.writeString(value.getId().toString());
		}
		else {
			gen.writeString(((NamespacedId) value.getId()).toStringWithoutDataset());
		}
	}

	@Override
	public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
		final NsIdRef idRefAnno = property.getAnnotation(NsIdRef.class);

		return new IdReferenceSerializer(idRefAnno == null || idRefAnno.withDataset());
	}
}
