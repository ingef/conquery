package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;

@CPSType(id = "CALCULATE_CBLOCKS", base = WorkerMessage.class)
public class StartCalculateCblocks extends WorkerMessage {
	@Override
	public void react(Worker context) throws Exception {
		context.getBucketManager().fullUpdate();
	}
}
