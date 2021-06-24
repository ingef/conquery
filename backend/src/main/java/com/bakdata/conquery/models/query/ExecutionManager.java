package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteQuery;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ExecutionManager {

	@NonNull
	private final Namespace namespace;

	private final Cache<ManagedExecutionId, List<List<EntityResult>>> executionResults = CacheBuilder.newBuilder()
																									 .softValues()
																									 .removalListener(this::executionRemoved)
																									 .build();

	private void executionRemoved(RemovalNotification<ManagedExecutionId, List<?>> removalNotification) {

		// If removal was done intentionally we assume it was also handled properly
		if(removalNotification.getCause() == RemovalCause.EXPLICIT){
			return;
		}

		final ManagedExecutionId executionId = removalNotification.getKey();

		final ManagedExecution<?> execution = namespace.getNamespaces().getMetaStorage().getExecution(executionId);

		// Execution might've already been deleted
		if(execution == null) {
			return;
		}

		log.warn("Evicted Results for Query[{}] (Reason: {})", executionId, removalNotification.getCause());

		execution.setState(ExecutionState.NEW);
	}


	// TODO make this instance method instead.
	public ManagedExecution<?> runQuery(DatasetRegistry datasets, QueryDescription query, User user, Dataset submittedDataset, ConqueryConfig config) {
		final ManagedExecution<?> execution = createExecution(datasets, query, user, submittedDataset);
		execute(datasets, execution, config);

		return execution;
	}

	public void execute(DatasetRegistry datasets, ManagedExecution<?> execution, ConqueryConfig config) {
		// Initialize the query / create subqueries
		execution.initExecutable(datasets, config);

		log.info("Executing Query[{}] in Datasets[{}]", execution.getQueryId(), execution.getRequiredDatasets());

		execution.start();

		final MetaStorage storage = datasets.getMetaStorage();
		final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(execution.getOwner(), storage).map(Group::getName).orElse("none");
		ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).inc();

		for (Namespace namespace : execution.getRequiredDatasets()) {
			namespace.sendToAll(new ExecuteQuery(execution));
		}
	}

	public ManagedExecution<?> createExecution(DatasetRegistry datasets, QueryDescription query, User user, Dataset submittedDataset) {
		return createQuery(datasets, query, UUID.randomUUID(), user, submittedDataset);
	}

	public ManagedExecution<?> createQuery(DatasetRegistry datasets, QueryDescription query, UUID queryId, User user, Dataset submittedDataset) {
		// Transform the submitted query into an initialized execution
		ManagedExecution<?> managed = query.toManagedExecution(user, submittedDataset);

		managed.setQueryId(queryId);

		// Store the execution
		datasets.getMetaStorage().addExecution(managed);

		return managed;
	}


	/**
	 * Receive part of query result and store into query.
	 *
	 * @param result
	 */
	public <R extends ShardResult, E extends ManagedExecution<R>> void handleQueryResult(R result) {
		final MetaStorage storage = namespace.getNamespaces().getMetaStorage();

		final E query = (E) getQuery(result.getQueryId());

		if(!query.getState().equals(ExecutionState.RUNNING)){
			return;
		}

		query.addResult(storage, result);

		if (query.getState() == ExecutionState.DONE || query.getState() == ExecutionState.FAILED) {
			final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(query.getOwner(), storage).map(Group::getName).orElse("none");

			ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).dec();
			ExecutionMetrics.getQueryStateCounter(query.getState(), primaryGroupName).inc();
			ExecutionMetrics.getQueriesTimeHistogram(primaryGroupName).update(query.getExecutionTime().toMillis());
		}
	}

	public ManagedExecution<?> getQuery(@NonNull ManagedExecutionId id) {
		return Objects.requireNonNull(namespace.getStorage().getMetaStorage().getExecution(id), "Unable to find query " + id.toString());
	}

	@SneakyThrows//TODO handle properly
	public void addQueryResult(ManagedExecutionId id, List<EntityResult> queryResults) {
		executionResults.get(id, ArrayList::new)
						.add(queryResults);
	}

	public void clearQueryResults(ManagedExecutionId id) {
		executionResults.invalidate(id);
	}

	@SneakyThrows//TODO handle properly
	public Stream<EntityResult> getQueryResults(ManagedExecutionId id) {
		return executionResults.get(id, Collections::emptyList)
							   .stream()
							   .flatMap(List::stream);
	}
}
