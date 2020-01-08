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
		switch (p.getCurrentTokenId()) {
			case JsonTokenId.ID_STRING: // let's do implicit re-parse
				String text = p.getText().trim();
				return Currency.getInstance(text);
			default:
				return (Currency) ctxt.handleUnexpectedToken(_valueClass, p);
		}
	}

	

}
