package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import javax.money.CurrencyUnit;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CurrencyUnitSerializer extends StdSerializer<CurrencyUnit> {

	private static final long serialVersionUID = 1L;

	public CurrencyUnitSerializer() {
		super(CurrencyUnit.class);
	}

	@Override
	public void serialize(CurrencyUnit value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeString(value.getCurrencyCode());
	}

}
