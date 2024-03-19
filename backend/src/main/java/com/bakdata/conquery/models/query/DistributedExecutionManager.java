package com.bakdata.conquery.models.query;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.specific.CancelQuery;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerHandler;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class DistributedExecutionManager implements ExecutionManager {

	private final MetaStorage storage;
	private final ClusterState clusterState;

	private final Cache<ManagedExecutionId, Map<WorkerId, List<EntityResult>>> executionResults =
			CacheBuilder.newBuilder()
						.softValues()
						.removalListener(this::executionRemoved)
						.build();

	public ManagedExecution getExecution(ManagedExecutionId execution) {
		return storage.getExecution(execution);
	}


	/**
	 * Manage state of evicted Queries, setting them to NEW.
	 */
	private void executionRemoved(RemovalNotification<ManagedExecutionId, Map<?,?>> removalNotification) {
		// If removal was done manually we assume it was also handled properly
		if (!removalNotification.wasEvicted()) {
			return;
		}

		final ManagedExecutionId executionId = removalNotification.getKey();

		log.warn("Evicted Results for Query[{}] (Reason: {})", executionId, removalNotification.getCause());

		final ManagedExecution execution = storage.getExecution(executionId);

		// The query might already be deleted
		if (execution != null) {
			execution.reset();
		}
	}

	@Override
	public ManagedExecution runQuery(Namespace namespace, QueryDescription query, User user, Dataset submittedDataset, ConqueryConfig config, boolean system) {
		final ManagedExecution execution = createExecution(query, user, submittedDataset, system);
		execute(namespace, execution, config);

		return execution;

	}

	@Override
	public ManagedExecution createExecution(QueryDescription query, User user, Dataset submittedDataset, boolean system) {
		return createQuery(query, UUID.randomUUID(), user, submittedDataset, system);
	}

	@Override
	public void execute(Namespace namespace, ManagedExecution execution, ConqueryConfig config) {

		clearQueryResults(execution);

		try {
			execution.initExecutable(namespace, config);
		}
		catch (Exception e) {
			// ConqueryErrors are usually user input errors so no need to log them at level=ERROR
			if (e instanceof ConqueryError) {
				log.warn("Failed to initialize Query[{}]", execution.getId(), e);
			}
			else {
				log.error("Failed to initialize Query[{}]", execution.getId(), e);
			}

			storage.removeExecution(execution.getId());
			throw e;
		}

		log.info("Starting execution[{}]", execution.getQueryId());

		execution.start();

		final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(execution.getOwner(), storage).map(Group::getName).orElse("none");
		ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).inc();

		if (execution instanceof InternalExecution<?> internalExecution) {
			doExecute(namespace, execution, internalExecution);
		}
	}

	private void doExecute(Namespace namespace, ManagedExecution execution, InternalExecution<?> internalExecution) {
		log.info("Executing Query[{}] in Dataset[{}]", execution.getQueryId(), namespace.getDataset().getId());

		final WorkerHandler workerHandler = getWorkerHandler(execution);

		workerHandler.sendToAll(internalExecution.createExecutionMessage());
	}

	private WorkerHandler getWorkerHandler(ManagedExecution execution) {
		return clusterState.getWorkerHandlers()
						   .get(execution.getDataset().getId());
	}

	// Visible for testing
	public ManagedExecution createQuery(QueryDescription query, UUID queryId, User user, Dataset submittedDataset, boolean system) {
		// Transform the submitted query into an initialized execution
		ManagedExecution managed = query.toManagedExecution(user, submittedDataset, storage);
		managed.setSystem(system);
		managed.setQueryId(queryId);

		// Store the execution
		storage.addExecution(managed);

		return managed;
	}

	/**
	 * Receive part of query result and store into query.
	 *
	 * @implNote subQueries of Forms are managed by the form itself, so need to be passed from outside.
	 */

	@SneakyThrows
	public <R extends ShardResult, E extends ManagedExecution & InternalExecution<R>> void handleQueryResult(R result, E query) {


		log.debug("Received Result[size={}] for Query[{}]", result.getResults().size(), result.getQueryId());
		log.trace("Received Result\n{}", result.getResults());

		if (query.getState() != ExecutionState.RUNNING) {
			return;
		}

		if(result.getError().isPresent()){
			query.fail(result.getError().get());
		}
		else {

			// We don't collect all results together into a fat list as that would cause lots of huge re-allocations for little gain.
			final Map<WorkerId, List<EntityResult>> results = executionResults.get(query.getId(), ConcurrentHashMap::new);
			results.put(result.getWorkerId(), result.getResults());

			final Set<WorkerId> finishedWorkers = results.keySet();

			// If all known workers have returned a result, the query is DONE.
			if (finishedWorkers.equals(getWorkerHandler(query).getAllWorkerIds())) {
				query.finish(ExecutionState.DONE);
			}
		}

		// State changed to DONE or FAILED
		if (query.getState() != ExecutionState.RUNNING) {
			final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(query.getOwner(), storage).map(Group::getName).orElse("none");

			ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).dec();
			ExecutionMetrics.getQueryStateCounter(query.getState(), primaryGroupName).inc();
			ExecutionMetrics.getQueriesTimeHistogram(primaryGroupName).update(query.getExecutionTime().toMillis());

			/* This log is here to prevent an NPE which could occur when no strong reference to result.getResults()
			 existed anymore after the query finished and immediately was reset */
			log.trace("Collected metrics for execution {}. Last result received: {}:", result.getQueryId(), result.getResults());
		}

	}



	/**
	 * Discard the query's results.
	 */
	@Override
	public void clearQueryResults(ManagedExecution execution) {
		executionResults.invalidate(execution.getId());
	}

	@Override
	public Stream<EntityResult> streamQueryResults(ManagedExecution execution) {
		final Map<?, List<EntityResult>> resultParts = executionResults.getIfPresent(execution.getId());

		return resultParts == null
			   ? Stream.empty()
			   : resultParts.values().stream().flatMap(List::stream);

	}

	@Override
	public void cancelQuery(Dataset dataset, ManagedExecution query) {
		log.debug("Sending cancel message to all workers.");

		getWorkerHandler(query).sendToAll(new CancelQuery(query.getId()));
	}

}
