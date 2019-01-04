package com.bakdata.conquery.io.jackson;

import javax.money.CurrencyUnit;

import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitDeserializer;
import com.bakdata.conquery.io.jackson.serializer.CurrencyUnitSerializer;
import com.bakdata.conquery.io.jackson.serializer.IdKeyDeserializer;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.PackageVersion;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class ConquerySerializersModule extends SimpleModule {

	private static final long serialVersionUID = 1L;

	public ConquerySerializersModule() {
		super("Conquery Module", PackageVersion.VERSION);
		addDeserializer(CurrencyUnit.class, new CurrencyUnitDeserializer());
		addSerializer(CurrencyUnit.class, new CurrencyUnitSerializer());
		
		//register IdKeySerializer for all id types
		ScanResult scanRes = new ClassGraph()
			.enableClassInfo()
			//blacklist some packages that contain large libraries
			.blacklistPackages(
				"groovy",
				"org.codehaus.groovy",
				"org.apache",
				"org.eclipse",
				"com.google"
			)
			.scan();
		
		for(Class<?> type : scanRes.getClassesImplementing(IId.class.getName()).loadClasses()) {
			addKeyDeserializer(type, new IdKeyDeserializer<>());
		}
	}
}
