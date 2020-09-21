package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import com.bakdata.conquery.models.common.BitMapCDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class CDateSetDeserializer extends StdDeserializer<BitMapCDateSet> {

	private static final long serialVersionUID = 1L;

	public CDateSetDeserializer() {
		super(BitMapCDateSet.class);
	}

	@Override
	public BitMapCDateSet deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		if (p.currentToken() == JsonToken.START_ARRAY) {
			int[] ints = p.readValueAs(int[].class);
			
			BitMapCDateSet set = BitMapCDateSet.create();
			for(int i=0; i<ints.length; i+=2) {
				set.add(CDateRange.of(ints[i], ints[i+1]));
			}
			return set;
		}
		else if(p.currentToken() == JsonToken.VALUE_STRING) {
			return CDateSet.parse(p.readValueAs(String.class));
		}
		else {
			return (BitMapCDateSet) ctxt.handleUnexpectedToken(BitMapCDateSet.class, p.currentToken(), p, "can't deserialize ICDateSet");
		}
	}
}
