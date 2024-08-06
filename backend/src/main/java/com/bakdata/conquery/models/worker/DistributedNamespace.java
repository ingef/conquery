package com.bakdata.conquery.models.worker;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.cluster.ClusterEntityResolver;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.messages.namespaces.specific.CollectColumnValuesJob;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateMatchingStatsMessage;
import com.bakdata.conquery.models.query.DistributedExecutionManager;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.models.query.entity.Entity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


/**
 * Keep track of all data assigned to a single dataset. Each ShardNode has one {@link Worker} per {@link Dataset} / {@link DistributedNamespace}.
 * Every Worker is assigned a partition of the loaded {@link Entity}s via {@link Entity::getBucket}.
 */
@Slf4j
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class DistributedNamespace extends Namespace {

	private final WorkerHandler workerHandler;
	private final DistributedExecutionManager executionManager;

	public DistributedNamespace(
			ObjectMapper preprocessMapper,
			ObjectMapper communicationMapper,
			NamespaceStorage storage,
			DistributedExecutionManager executionManager,
			JobManager jobManager,
			FilterSearch filterSearch,
			IndexService indexService,
			ClusterEntityResolver clusterEntityResolver,
			List<Injectable> injectables,
			WorkerHandler workerHandler
	) {
		super(preprocessMapper, communicationMapper, storage, executionManager, jobManager, filterSearch, indexService, clusterEntityResolver, injectables);
		this.executionManager = executionManager;
		this.workerHandler = workerHandler;
	}

	@Override
	void updateMatchingStats() {
		final Collection<Concept<?>> concepts = getStorage().getAllConcepts()
															.stream()
															.filter(concept -> concept.getMatchingStats() == null)
															.collect(Collectors.toSet());
		getWorkerHandler().sendToAll(new UpdateMatchingStatsMessage(concepts));
	}

	@Override
	void registerColumnValuesInSearch(Set<Column> columns) {
		log.trace("Sending columns to collect values on shards: {}", Arrays.toString(columns.toArray()));
		getWorkerHandler().sendToAll(new CollectColumnValuesJob(columns, this));
	}

}
