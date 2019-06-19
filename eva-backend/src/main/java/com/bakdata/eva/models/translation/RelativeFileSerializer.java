package com.bakdata.eva.models.translation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

public class RelativeFileSerializer extends StdScalarSerializer<File> {
	public RelativeFileSerializer() {
		super(File.class);
	}

	@Override
	public void serialize(File value, JsonGenerator g, SerializerProvider provider) throws IOException {
		g.writeString(value.getPath());
	}

	@Override
	public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
		return createSchemaNode("string", true);
	}
	
	@Override
	public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
		visitStringFormat(visitor, typeHint);
	}
}