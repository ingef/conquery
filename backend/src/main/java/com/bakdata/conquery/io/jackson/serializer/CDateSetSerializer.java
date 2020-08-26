package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.Collection;

import com.bakdata.conquery.models.common.ICDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CDateSetSerializer extends StdSerializer<ICDateSet> {

	private static final long serialVersionUID = 1L;

	public CDateSetSerializer() {
		super(ICDateSet.class);
	}

	@Override
	public void serialize(ICDateSet value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		final Collection<CDateRange> ranges = value.asRanges();

		gen.writeStartArray(ranges.size() * 2);

		for(CDateRange range : ranges) {
			gen.writeNumber(range.getMinValue());
			gen.writeNumber(range.getMaxValue());
		}

		gen.writeEndArray();
	}
}