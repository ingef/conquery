package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@CPSType(id = "IMPORT_BIT", base = NamespacedMessage.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@Getter
@Setter
@Slf4j
public class ImportBucket extends WorkerMessage {

	private final String name;

	private final Bucket bucket;

	public static ImportBucket forBucket(Bucket bucket)  {
		return new ImportBucket(bucket.getId().toString(),bucket);
	}

	@Override
	public void react(Worker context) throws Exception {
		log.info("Received {}", bucket.getId());

		context.addBucket(bucket);
	}

	@Override
	public String toString() {
		return String.format("Importing Bucket[%s]", getName());
	}
}
