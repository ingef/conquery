package com.bakdata.conquery.io.jackson;

import javax.money.CurrencyUnit;

import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitDeserializer;
import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.PackageVersion;

public class ConquerySerializersModule extends SimpleModule {

	private static final long serialVersionUID = 1L;


	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ConquerySerializersModule() {
		super("Conquery Module", PackageVersion.VERSION);
		addDeserializer(CurrencyUnit.class, new CurrencyUnitDeserializer());
		addSerializer(CurrencyUnit.class, new CurrencyUnitSerializer());
	}
}
