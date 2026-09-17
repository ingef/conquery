package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

@SuppressWarnings("rawtypes")
public class ClassToInstanceMapDeserializer extends StdDeserializer<ClassToInstanceMap> {

	public ClassToInstanceMapDeserializer() {
		super(ClassToInstanceMap.class);
	}

	@Override
	public ClassToInstanceMap<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ClassToInstanceMap map = MutableClassToInstanceMap.create();
		if(p.currentToken()==JsonToken.START_OBJECT) {
			p.nextToken();
		}
		while(true) {
			if(p.currentToken()==JsonToken.END_OBJECT) {
				return map;
			}
			String typeName = p.getCurrentName();
			if(typeName == null) {
				return (ClassToInstanceMap<?>)ctxt.handleUnexpectedToken(ClassToInstanceMap.class, p);
			}
			try {
				Class<?> type = Class.forName(typeName);
				p.nextToken();
				Object value = ctxt.readValue(p, type);
				p.nextToken();
				map.putInstance(type, value);
			}
			catch (ClassNotFoundException e) {
				throw new RuntimeException("Could not resolve class "+typeName, e);
			}
		}
	}

}
