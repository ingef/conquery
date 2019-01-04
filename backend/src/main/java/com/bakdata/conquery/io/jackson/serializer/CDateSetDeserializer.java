package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class CDateSetDeserializer extends StdDeserializer<CDateSet> {

	private static final long serialVersionUID = 1L;

	public CDateSetDeserializer() {
		super(CDateSet.class);
	}

	@Override
	public CDateSet deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		int[] ints = p.readValueAs(int[].class);
		
		CDateSet set = CDateSet.create();
		for(int i=0; i<ints.length; i+=2) {
			set.add(new CDateRange(ints[i], ints[i+1]));
		}
		return set;
	}
}
