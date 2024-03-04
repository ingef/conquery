package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.Collection;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.jobs.WorkerUpdateMatchingStatsJob;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * For each {@link com.bakdata.conquery.models.query.queryplan.specific.ConceptNode} calculate the number of matching events and the span of date-ranges.
 */
@CPSType(id = "UPDATE_MATCHING_STATS", base = NamespacedMessage.class)
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
public class UpdateMatchingStatsMessage extends WorkerMessage {

	@NsIdRefCollection
	private final Collection<Concept<?>> concepts;


	@Override
	public void react(Worker worker) throws Exception {
		worker.getJobManager().addSlowJob(new WorkerUpdateMatchingStatsJob(worker, concepts));
	}

}
