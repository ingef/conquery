package com.bakdata.conquery.mode;

import javax.annotation.Nullable;
import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
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
	private DatasetRegistry<? extends Namespace> datasetRegistry = null;
	private MetaStorage storage = null;

	public void init(DatasetRegistry<? extends Namespace> datasetRegistry) {
		this.datasetRegistry = datasetRegistry;
		this.storage = datasetRegistry.getMetaStorage();
	}

	public ObjectMapper createInternalObjectMapper(@Nullable Class<? extends View> viewClass) {
		if (datasetRegistry == null || storage == null) {
			throw new IllegalStateException("%s must be initialized by calling its init method".formatted(this.getClass().getSimpleName()));
		}

		final ObjectMapper objectMapper = getConfig().configureObjectMapper(Jackson.BINARY_MAPPER.copy());

		final MutableInjectableValues injectableValues = new MutableInjectableValues();
		objectMapper.setInjectableValues(injectableValues);
		injectableValues.add(Validator.class, getValidator());
		getDatasetRegistry().injectInto(objectMapper);
		getStorage().injectInto(objectMapper);
		getConfig().injectInto(objectMapper);


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
