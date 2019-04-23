package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.models.jobs.UpdateMatchingStats;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;

public class UpdateMatchingStatsMessage extends WorkerMessage {

	@Override
	public void react(Worker context) throws Exception {
		context.getJobManager().addSlowJob(new UpdateMatchingStats(context));
	}
}
