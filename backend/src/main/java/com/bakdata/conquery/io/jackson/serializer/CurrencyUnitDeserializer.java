package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.Currency;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

public class CurrencyUnitDeserializer extends StdScalarDeserializer<Currency> {

	private static final long serialVersionUID = 1L;

	public CurrencyUnitDeserializer() {
		super(String.class);
	}
	
	@Override
	public Currency deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		if (p.currentTokenId() != JsonTokenId.ID_STRING) {
			return (Currency) ctxt.handleUnexpectedToken(_valueClass, p);
		}
		// let's do implicit re-parse
		final String text = p.getText().trim();
		return Currency.getInstance(text);
	}

	

}
