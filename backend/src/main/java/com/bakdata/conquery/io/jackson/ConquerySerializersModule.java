package com.bakdata.conquery.io.jackson;

import java.io.IOException;
import java.util.Currency;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.serializer.ClassToInstanceMapDeserializer;
import com.bakdata.conquery.io.jackson.serializer.ConqueryDoubleSerializer;
import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitDeserializer;
import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitSerializer;
import com.bakdata.conquery.io.jackson.serializer.IdKeyDeserializer;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.PackageVersion;
import com.google.common.collect.BiMap;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.HashBiMap;
import groovyjarjarantlr4.v4.runtime.misc.IntSet;
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
		addAbstractTypeMapping(BiMap.class, HashBiMap.class);
		addValueInstantiator(HashBiMap.class, new ValueInstantiator.Base(HashBiMap.class) {
			@Override
			public boolean canCreateUsingDefault() {
				return true;
			}
			
			@Override
			public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
				return HashBiMap.create();
			}
		});
		addDeserializer(ClassToInstanceMap.class, new ClassToInstanceMapDeserializer());

		//register IdKeySerializer for all id types
		List<Class<?>> idTypes = CPSTypeIdResolver
			.SCAN_RESULT
			.getClassesImplementing(IId.class.getName())
			.loadClasses();

		for(Class<?> type : idTypes) {
			addKeyDeserializer(type, new IdKeyDeserializer<>());
		}
		addSerializer(new ConqueryDoubleSerializer());
	}
}