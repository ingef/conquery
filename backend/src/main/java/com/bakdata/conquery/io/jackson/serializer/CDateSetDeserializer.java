package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import com.bakdata.conquery.models.common.BitMapCDateSet;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.ICDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class CDateSetDeserializer extends StdDeserializer<ICDateSet> {

	private static final long serialVersionUID = 1L;

	public CDateSetDeserializer() {
		super(ICDateSet.class);
	}

	@Override
	public ICDateSet deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		if (p.currentToken() == JsonToken.START_ARRAY) {
			int[] ints = p.readValueAs(int[].class);
			
			ICDateSet set = BitMapCDateSet.create();
			for(int i=0; i<ints.length; i+=2) {
				set.add(CDateRange.of(ints[i], ints[i+1]));
			}
			return set;
		}
		else if(p.currentToken() == JsonToken.VALUE_STRING) {
			return CDateSet.parse(p.readValueAs(String.class));
		}
		else {
			return (ICDateSet) ctxt.handleUnexpectedToken(ICDateSet.class, p.currentToken(), p, "can't deserialize ICDateSet");
		}
	}
}
