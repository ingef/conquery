package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
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
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
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

	private final Cache<ManagedExecutionId, List<List<EntityResult>>> executionResults =
			CacheBuilder.newBuilder()
						.softValues()
						.removalListener(this::executionRemoved)
						.build();

	/**
	 * Manage state of evicted Queries, setting them to NEW.
	 */
	private void executionRemoved(RemovalNotification<ManagedExecutionId, List<?>> removalNotification) {
		// If removal was done manually we assume it was also handled properly
		if (!removalNotification.wasEvicted()) {
			return;
		}

		final ManagedExecutionId executionId = removalNotification.getKey();

		log.warn("Evicted Results for Query[{}] (Reason: {})", executionId, removalNotification.getCause());

		storage.getExecution(executionId).reset();
	}

	@Override
	public ManagedExecution runQuery(Namespace namespace, QueryDescription query, User user, Dataset submittedDataset, ConqueryConfig config, boolean system) {
		final ManagedExecution execution = createExecution(query, user, submittedDataset, system);
		execute(namespace, execution, config);

		return execution;

	}

	@Override
	public void execute(Namespace namespace, ManagedExecution execution, ConqueryConfig config) {
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
			log.info("Executing Query[{}] in Dataset[{}]", execution.getQueryId(), namespace.getDataset().getId());
			clusterState.getWorkerHandlers().get(execution.getDataset().getId()).sendToAll(internalExecution.createExecutionMessage());
		}
	}

	@Override
	public ManagedExecution createExecution(QueryDescription query, User user, Dataset submittedDataset, boolean system) {
		return createQuery(query, UUID.randomUUID(), user, submittedDataset, system);
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
	 * @param result
	 */
	public <R extends ShardResult, E extends ManagedExecution & InternalExecution<R>> void handleQueryResult(R result) {

		final E query = (E) storage.getExecution(result.getQueryId());

		if (query.getState() != ExecutionState.RUNNING) {
			return;
		}

		query.addResult(result);

		// State changed to DONE or FAILED
		if (query.getState() != ExecutionState.RUNNING) {
			final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(query.getOwner(), storage).map(Group::getName).orElse("none");

			ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).dec();
			ExecutionMetrics.getQueryStateCounter(query.getState(), primaryGroupName).inc();
			ExecutionMetrics.getQueriesTimeHistogram(primaryGroupName).update(query.getExecutionTime().toMillis());
		}

	}


	/**
	 * Register another result for the execution.
	 */

	@SneakyThrows(ExecutionException.class) // can only occur if ArrayList::new fails which is unlikely and would have other problems also
	public void addQueryResult(ManagedExecution execution, List<EntityResult> queryResults) {
		// We don't collect all results together into a fat list as that would cause lots of huge re-allocations for little gain.
		executionResults.get(execution.getId(), ArrayList::new)
						.add(queryResults);
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
		final List<List<EntityResult>> resultParts = executionResults.getIfPresent(execution.getId());

		return resultParts == null
			   ? Stream.empty()
			   : resultParts.stream().flatMap(List::stream);

	}

	@Override
	public void cancelQuery(Dataset dataset, ManagedExecution query) {
		query.cancel();
	}

}
