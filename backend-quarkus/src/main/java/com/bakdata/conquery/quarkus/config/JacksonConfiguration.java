package com.bakdata.conquery.quarkus.config;

import com.bakdata.conquery.quarkus.models.PolymorphicModelRegistry;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class JacksonConfiguration implements ObjectMapperCustomizer {

	@Inject
	PolymorphicModelRegistry modelRegistry;

	@Override
	public void customize(ObjectMapper objectMapper) {
		objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		modelRegistry.allTypes().forEach(type -> objectMapper.registerSubtypes(new NamedType(type.modelType(), type.typeId())));
	}
}
