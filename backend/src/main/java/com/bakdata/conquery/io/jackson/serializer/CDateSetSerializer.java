package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CDateSetSerializer extends StdSerializer<CDateSet> {

	private static final long serialVersionUID = 1L;

	public CDateSetSerializer() {
		super(CDateSet.class);
	}

	@Override
	public void serialize(CDateSet value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeStartArray(value.asRanges().size());
		for(CDateRange range : value.asRanges()) {
			gen.writeStartArray(2);
			gen.writeNumber(range.getMinValue());
			gen.writeNumber(range.getMaxValue());
			gen.writeEndArray();
		}
		gen.writeEndArray();
	}
}