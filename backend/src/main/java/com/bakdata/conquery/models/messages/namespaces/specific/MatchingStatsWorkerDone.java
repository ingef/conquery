package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import lombok.Data;

@Data
public class MatchingStatsWorkerDone extends NamespaceMessage {

	private final WorkerId workerId;

	@Override
	public void react(DistributedNamespace context) throws Exception {
		context.matchingStatsWorkerFinished(getWorkerId());
	}
}
