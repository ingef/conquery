package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.jobs.UpdateMatchingStats;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;

@CPSType(id = "UPDATE_MATCHING_STATS", base = NamespacedMessage.class)
public class UpdateMatchingStatsMessage extends WorkerMessage.Slow {

	@Override
	public void react(Worker context) throws Exception {
		context.getJobManager().addSlowJob(new UpdateMatchingStats(context));
	}
}
