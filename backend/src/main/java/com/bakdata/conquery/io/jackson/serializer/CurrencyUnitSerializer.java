package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.Currency;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CurrencyUnitSerializer extends StdSerializer<Currency> {

	private static final long serialVersionUID = 1L;

	public CurrencyUnitSerializer() {
		super(Currency.class);
	}

	@Override
	public void serialize(Currency value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeString(value.getCurrencyCode());
	}

}
