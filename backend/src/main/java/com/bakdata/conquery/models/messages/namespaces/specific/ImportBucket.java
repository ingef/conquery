package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@CPSType(id = "IMPORT_BIT", base = NamespacedMessage.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@Getter
@Setter
@Slf4j
public class ImportBucket extends WorkerMessage.Slow {

	private final String name;

	/**
	 * @implSpec We need to deserialize this lazily, as this message and it's dependencies are transmitted in such quick succession, that they are sometimes not there on deserialization of the message.
	 */
	private final byte[] rawBucket;

	public static ImportBucket forBucket(Bucket bucket) throws JsonProcessingException {
		return new ImportBucket(bucket.getId().toString(), Jackson.BINARY_MAPPER.writerWithView(InternalOnly.class).writeValueAsBytes(bucket));
	}

	@Override
	public void react(Worker context) throws Exception {
		final ObjectMapper objectMapper = context.getStorage().getDataset().injectInto(context.inject(Jackson.BINARY_MAPPER));

		final Bucket bucket = objectMapper.readValue(rawBucket, Bucket.class);

		log.trace("Received {}", bucket.getId());

		context.addBucket(bucket);
	}

	@Override
	public String toString() {
		return String.format("Importing Bucket[%s]", getName());
	}
}
