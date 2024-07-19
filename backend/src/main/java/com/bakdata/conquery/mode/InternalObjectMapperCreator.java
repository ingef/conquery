package com.bakdata.conquery.mode;

import javax.annotation.Nullable;
import jakarta.validation.Validator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Creator for internal object mapper in the manager.
 */
@Getter
@RequiredArgsConstructor
public class InternalObjectMapperCreator {
	private final ConqueryConfig config;
	private final Validator validator;

	public ObjectMapper createInternalObjectMapper(@Nullable Class<? extends View> viewClass, Injectable ... injectables) {

		final ObjectMapper objectMapper = getConfig().configureObjectMapper(Jackson.copyMapperAndInjectables(Jackson.BINARY_MAPPER.copy()));

		final MutableInjectableValues injectableValues = new MutableInjectableValues();

		objectMapper.setInjectableValues(injectableValues);
		injectableValues.add(Validator.class, getValidator());

		getConfig().injectInto(objectMapper);

		for (Injectable injectable : injectables) {
			injectable.injectInto(objectMapper);
		}



		if (viewClass != null) {
			// Set serialization config
			SerializationConfig serializationConfig = objectMapper.getSerializationConfig();

			serializationConfig = serializationConfig.withView(viewClass);

			objectMapper.setConfig(serializationConfig);

			// Set deserialization config
			DeserializationConfig deserializationConfig = objectMapper.getDeserializationConfig();

			deserializationConfig = deserializationConfig.withView(viewClass);

			objectMapper.setConfig(deserializationConfig);
		}

		return objectMapper;
	}
}
