package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@CPSType(id = "REQUEST_CONSISTENCY", base = NamespacedMessage.class)
@Setter
@Getter
@Slf4j
@EqualsAndHashCode
public class RequestConsistency extends WorkerMessage {

	@Override
	public void react(Worker context) throws Exception {
		log.info("BEGIN Gather consistency information");

		// Gather ImportIds
		Set<ImportId> workerImports = context.getStorage().getAllImports().collect(Collectors.toSet());

		// Gather BucketIds
		Set<BucketId> workerBuckets = context.getStorage().getAllBucketIds().collect(Collectors.toSet());

		// Send report
		context.send(new ReportConsistency(context.getInfo().getId(), workerImports, workerBuckets));

		log.debug("FINISHED Gather consistency information");
	}
}
