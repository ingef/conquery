package com.bakdata.conquery.io.jackson;

import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitDeserializer;
import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitSerializer;
import com.bakdata.conquery.io.jackson.serializer.IdKeyDeserializer;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.PackageVersion;
import io.github.classgraph.ClassGraph;

import javax.money.CurrencyUnit;
import java.util.List;

public class ConquerySerializersModule extends SimpleModule {

	private static final long serialVersionUID = 1L;

	public ConquerySerializersModule() {
		super("Conquery Module", PackageVersion.VERSION);
		addDeserializer(CurrencyUnit.class, new CurrencyUnitDeserializer());
		addSerializer(CurrencyUnit.class, new CurrencyUnitSerializer());

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
		for (Class<?> type : idTypes) {
			addKeyDeserializer(type, new IdKeyDeserializer<>());
		}
	}
}
