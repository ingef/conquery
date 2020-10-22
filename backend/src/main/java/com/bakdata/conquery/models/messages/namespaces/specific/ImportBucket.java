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

@CPSType(id = "IMPORT_BIT", base = NamespacedMessage.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@Getter
@Setter
public class ImportBucket extends WorkerMessage.Slow {

	private final Bucket bucket;

	@Override
	public void react(Worker context) throws Exception {
		// todo get import via idRef instead.
		bucket.setImp(context.getStorage().getImport(bucket.getImportId()));
		context.getStorage().addBucket(bucket);
	}

	@Override
	public String toString() {
		return String.format("Importing Bucket[%s]", bucket.getId());
	}
}
