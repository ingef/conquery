package com.bakdata.conquery.io.jackson;

import java.util.Currency;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitDeserializer;
import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitSerializer;
import com.bakdata.conquery.io.jackson.serializer.IdKeyDeserializer;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.PackageVersion;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class ConquerySerializersModule extends SimpleModule {

	private static final long serialVersionUID = 1L;
	public static final ConquerySerializersModule INSTANCE = new ConquerySerializersModule();

	private ConquerySerializersModule() {
		super("Conquery Module", PackageVersion.VERSION);
		addDeserializer(Currency.class, new CurrencyUnitDeserializer());
		addSerializer(Currency.class, new CurrencyUnitSerializer());
		addAbstractTypeMapping(Int2ObjectMap.class, Int2ObjectOpenHashMap.class);

		//register IdKeySerializer for all id types
		List<Class<?>> idTypes = CPSTypeIdResolver
			.SCAN_RESULT
			.getClassesImplementing(IId.class.getName())
			.loadClasses();

		for(Class<?> type : idTypes) {
			addKeyDeserializer(type, new IdKeyDeserializer<>());
		}
	}
}