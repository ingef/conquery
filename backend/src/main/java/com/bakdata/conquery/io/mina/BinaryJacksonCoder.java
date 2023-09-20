package com.bakdata.conquery.io.mina;

import javax.validation.Validator;

import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.bakdata.conquery.util.io.EndCheckableInputStream;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BinaryJacksonCoder implements CQCoder<NetworkMessage<?>> {

	private final Validator validator;
	private final ObjectWriter writer;
	private final ObjectReader reader;

	public BinaryJacksonCoder(IdResolveContext datasets, Validator validator, ObjectMapper objectMapper) {
		this.validator = validator;
		writer = objectMapper.writerFor(NetworkMessage.class);
		reader = datasets.injectIntoNew(objectMapper.readerFor(NetworkMessage.class)).without(Feature.AUTO_CLOSE_SOURCE);
	}

	@Override
	public Chunkable encode(NetworkMessage<?> message) throws Exception {
		ValidatorHelper.failOnError(log, validator.validate(message));

		return new Chunkable(message.getMessageId(), writer, message);
	}

	@Override
	public NetworkMessage<?> decode(ChunkedMessage message) throws Exception {
		try (EndCheckableInputStream is = message.createInputStream()) {
			final Object obj = reader.readValue(is);
			if (!is.isAtEnd()) {
				throw new IllegalStateException("After reading the JSON message " + obj + " the buffer has still bytes available");
			}
			ValidatorHelper.failOnError(log, validator.validate(obj));
			return (NetworkMessage<?>) obj;
		}
	}
}
