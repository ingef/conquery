package com.bakdata.conquery.io.mina;

import java.io.File;
import java.util.UUID;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class BinaryJacksonCoder implements CQCoder<NetworkMessage<?>> {

	private final Validator validator;
	private final ObjectWriter writer;
	private final ObjectReader reader;
	private final IdResolveContext resolveContext;

	public BinaryJacksonCoder(IdResolveContext datasets, Validator validator) {
		this.validator = validator;
		this.writer = Jackson.BINARY_MAPPER
							  .writerFor(NetworkMessage.class)
							  .withView(InternalOnly.class);

		this.reader = datasets.injectInto(Jackson.BINARY_MAPPER.readerFor(NetworkMessage.class))
							  .without(Feature.AUTO_CLOSE_SOURCE)
							  .withView(InternalOnly.class);
		this.resolveContext = datasets;
	}

	@Override
	public Chunkable encode(NetworkMessage<?> message) throws Exception {
		ValidatorHelper.failOnError(log, validator.validate(message), "encoding " + message.getClass().getSimpleName());

		UUID id = message.getMessageId();
		Chunkable chunkable = new Chunkable(id, writer, message);
		if (log.isTraceEnabled()) {
			Jackson.MAPPER.writerFor(NetworkMessage.class).with(SerializationFeature.INDENT_OUTPUT).writeValue(new File("dumps/out_" + id + ".json"), message);
		}
		return chunkable;
	}

	@Override
	public NetworkMessage<?> decode(ChunkedMessage message) throws Exception {
		return reader.readValue(message.getParser());
	}
}
