package com.bakdata.conquery.models.worker;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.cluster.ClusterEntityResolver;
import com.bakdata.conquery.models.config.ClusterConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.messages.namespaces.specific.CollectColumnValuesMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateMatchingStatsMessage;
import com.bakdata.conquery.models.query.DistributedExecutionManager;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
	private final ClusterConfig clusterConfig;

	public DistributedNamespace(
			ObjectMapper preprocessMapper,
			NamespaceStorage storage,
			DistributedExecutionManager executionManager,
			JobManager jobManager,
			SearchProcessor filterSearch,
			ClusterEntityResolver clusterEntityResolver,
			List<Injectable> injectables,
			WorkerHandler workerHandler,
			ClusterConfig clusterConfig) {
		super(preprocessMapper, storage, executionManager, jobManager, filterSearch, clusterEntityResolver, injectables);
		this.executionManager = executionManager;
		this.workerHandler = workerHandler;
		this.clusterConfig = clusterConfig;
	}

	@Override
	void updateMatchingStats() {
		try(Stream<Concept<?>> allConcepts = getStorage().getAllConcepts()) {
			final Collection<ConceptId> concepts = allConcepts
					.filter(concept -> concept.getMatchingStats() == null)
					.map(Concept::getId)
					.collect(Collectors.toSet());
			getWorkerHandler().sendToAll(new UpdateMatchingStatsMessage(concepts));
		}
	}

	@Override
	void registerColumnValuesInSearch(Set<Column> columns) {
		log.trace("Sending columns to collect values on shards: {}", Arrays.toString(columns.toArray()));

		final CollectColumnValuesMessage columnValuesJob = new CollectColumnValuesMessage(
				clusterConfig.getColumnValuesPerChunk(),
				columns.stream().map(Column::getId).collect(Collectors.toSet()), this
		);

		getWorkerHandler().sendToAll(columnValuesJob);
	}

}
