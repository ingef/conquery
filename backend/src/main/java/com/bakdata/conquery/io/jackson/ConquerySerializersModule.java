package com.bakdata.conquery.io.jackson;

import java.io.IOException;
import java.util.Currency;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.serializer.CharArrayBufferDeserializer;
import com.bakdata.conquery.io.jackson.serializer.CharArrayBufferSerializer;
import com.bakdata.conquery.io.jackson.serializer.ClassToInstanceMapDeserializer;
import com.bakdata.conquery.io.jackson.serializer.ConqueryDoubleSerializer;
import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitDeserializer;
import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitSerializer;
import com.bakdata.conquery.io.jackson.serializer.IdKeyDeserializer;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.PackageVersion;
import com.google.common.collect.BiMap;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.HashBiMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.http.util.CharArrayBuffer;

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
				.getSubclasses(AId.class.getName())
				.loadClasses();

		for(Class<?> type : idTypes) {
			addKeyDeserializer(type, new IdKeyDeserializer<>());
		}
		addSerializer(new ConqueryDoubleSerializer());
		addDeserializer(CharArrayBuffer.class, new CharArrayBufferDeserializer());
		addSerializer(CharArrayBuffer.class, new CharArrayBufferSerializer());
	}
}