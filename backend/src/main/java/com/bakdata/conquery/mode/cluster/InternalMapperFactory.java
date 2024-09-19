package com.bakdata.conquery.mode.cluster;

import jakarta.validation.Validator;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.ShardWorkers;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;

public record InternalMapperFactory(ConqueryConfig config, Validator validator) {

	public ObjectMapper createShardCommunicationMapper() {
		return createInternalObjectMapper(View.InternalCommunication.class);
	}

	public ObjectMapper createWorkerCommunicationMapper(ShardWorkers workers) {
		final ObjectMapper objectMapper = createInternalObjectMapper(View.InternalCommunication.class);

		workers.injectInto(objectMapper);

		return objectMapper;
	}

	public ObjectMapper createWorkerPersistenceMapper(ShardWorkers workers) {
		final ObjectMapper objectMapper = createInternalObjectMapper(View.Persistence.Shard.class);

		workers.injectInto(objectMapper);

		return objectMapper;
	}

	public ObjectMapper createNamespacePersistenceMapper(DatasetRegistry<?> datasetRegistry) {
		final ObjectMapper objectMapper = createInternalObjectMapper(View.Persistence.Manager.class);

		datasetRegistry.injectInto(objectMapper);

		return objectMapper;
	}

	public ObjectMapper createManagerPersistenceMapper(DatasetRegistry<?> datasetRegistry, MetaStorage metaStorage) {
		ObjectMapper objectMapper = createInternalObjectMapper(View.Persistence.Manager.class);

		datasetRegistry.injectInto(objectMapper);
		metaStorage.injectInto(objectMapper);

		return objectMapper;
	}

	public ObjectMapper createManagerCommunicationMapper(DatasetRegistry<?> datasetRegistry) {
		ObjectMapper objectMapper = createInternalObjectMapper(View.InternalCommunication.class);

		datasetRegistry.injectInto(objectMapper);

		return objectMapper;
	}



	public ObjectMapper createPreprocessMapper(DatasetRegistry<?> datasetRegistry) {
		ObjectMapper objectMapper = createInternalObjectMapper(null);

		datasetRegistry.injectInto(objectMapper);

		return objectMapper;
	}

	/**
	 * @return a preconfigured binary object mapper
	 */
	private ObjectMapper createInternalObjectMapper(Class<? extends View> viewClass) {
		final ObjectMapper objectMapper = config.configureObjectMapper(Jackson.copyMapperAndInjectables(Jackson.BINARY_MAPPER));

		final MutableInjectableValues injectableValues = new MutableInjectableValues();
		objectMapper.setInjectableValues(injectableValues);

		injectableValues.add(Validator.class, validator);
		config.injectInto(objectMapper);

		if (viewClass != null) {
			setViewClass(objectMapper, viewClass);
		}

		return objectMapper;
	}

	public static void setViewClass(ObjectMapper objectMapper, Class<? extends View> viewClass) {
		// Set serialization config
		SerializationConfig serializationConfig = objectMapper.getSerializationConfig();

		serializationConfig = serializationConfig.withView(viewClass);

		objectMapper.setConfig(serializationConfig);

		// Set deserialization config
		DeserializationConfig deserializationConfig = objectMapper.getDeserializationConfig();

		deserializationConfig = deserializationConfig.withView(viewClass);

		objectMapper.setConfig(deserializationConfig);
	}



	/**
	 * Customize the mapper from the environment, that is used in the REST-API.
	 * In contrast to the internal object mapper this uses textual JSON representation
	 * instead of the binary smile format. It also does not expose internal fields through serialization.
	 * <p>
	 * Internal and external mapper have in common that they might process the same classes/objects and that
	 * they are configured to understand certain Conquery specific data types.
	 *
	 * @param objectMapper to be configured (should be a JSON mapper)
	 */
	public void customizeApiObjectMapper(ObjectMapper objectMapper, DatasetRegistry<?> datasetRegistry, MetaStorage metaStorage) {

		InternalMapperFactory.setViewClass(objectMapper, View.Api.class);

		final MutableInjectableValues injectableValues = new MutableInjectableValues();
		objectMapper.setInjectableValues(injectableValues);
		injectableValues.add(Validator.class, validator);

		datasetRegistry.injectInto(objectMapper);
		metaStorage.injectInto(objectMapper);
		config.injectInto(objectMapper);
	}
}
