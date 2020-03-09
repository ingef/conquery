package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ConqueryDoubleSerializer extends JsonSerializer<Double> {

	@Override
	public Class<Double> handledType() {
		return Double.class;
	}
	
	@Override
	public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if(value.isInfinite() || value.isNaN()) {
			gen.writeNull();
		}
		else {
			gen.writeNumber(value.doubleValue());
		}
	}

}
