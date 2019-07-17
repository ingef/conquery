package com.bakdata.conquery.models.events.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.models.types.CType;
import com.github.powerlibraries.io.In;

public class TemplateTest {

	public static Set<Class<? extends CType>> findAllCTypes() {
		return CPSTypeIdResolver
			.listImplementations(CType.class);
	}
	
	@ParameterizedTest @MethodSource("findAllCTypes")
	public void checkIfTemplateExists(Class<? extends CType> type) throws IOException {
		assertThat(In.resource("/com/bakdata/conquery/models/events/generation/types/"+type.getSimpleName()+".ftl").readAll())
			.contains("<#macro nullValue type")
			.contains("<#macro kryoSerialization type")
			.contains("<#macro kryoDeserialization type")
			.contains("<#macro nullCheck type")
			.contains("<#macro majorTypeTransformation type");
	}
}
