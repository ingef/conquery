package com.bakdata.conquery.io.jackson;

import java.util.List;

import javax.money.CurrencyUnit;

import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitDeserializer;
import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitSerializer;
import com.bakdata.conquery.io.jackson.serializer.IdKeyDeserializer;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.PackageVersion;

import io.github.classgraph.ClassGraph;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class ConquerySerializersModule extends SimpleModule {

	private static final long serialVersionUID = 1L;

	public ConquerySerializersModule() {
		super("Conquery Module", PackageVersion.VERSION);
		addDeserializer(CurrencyUnit.class, new CurrencyUnitDeserializer());
		addSerializer(CurrencyUnit.class, new CurrencyUnitSerializer());
		addAbstractTypeMapping(Int2ObjectMap.class, Int2ObjectOpenHashMap.class);
		
		//register IdKeySerializer for all id types
		List<Class<?>> idTypes = new ClassGraph()
			.enableClassInfo()
			.blacklistPackages(
				"groovy",
				"org.codehaus.groovy",
				"org.apache",
				"org.eclipse",
				"com.google"
			)
			.scan()
			.getClassesImplementing(IId.class.getName())
			.loadClasses();

		for(Class<?> type : idTypes) {
			addKeyDeserializer(type, new IdKeyDeserializer<>());
		}
	}
}
