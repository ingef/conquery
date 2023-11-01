package com.bakdata.conquery.models.worker;

import java.util.List;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.jobs.JobManager;
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
public class DistributedNamespace extends LocalNamespace {

	private final WorkerHandler workerHandler;
	private final DistributedExecutionManager executionManager;


	public DistributedNamespace(ObjectMapper preprocessMapper, ObjectMapper communicationMapper, NamespaceStorage storage, DistributedExecutionManager executionManager,
								JobManager jobManager, FilterSearch filterSearch, IndexService indexService, List<Injectable> injectables,
								WorkerHandler workerHandler) {
		super(preprocessMapper, communicationMapper, storage, executionManager, jobManager, filterSearch, indexService, injectables);
		this.executionManager = executionManager;
		this.workerHandler = workerHandler;
	}


	public void matchingStatsWorkerFinished(WorkerId workerId) {
	}

	public void matchingStatsManagerFinished() {

	}
}
