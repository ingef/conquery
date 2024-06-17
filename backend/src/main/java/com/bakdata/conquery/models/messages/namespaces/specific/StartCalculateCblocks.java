package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;

public class StartCalculateCblocks extends WorkerMessage {
	@Override
	public void react(Worker context) throws Exception {
		context.getBucketManager().fullUpdate();
	}
}
