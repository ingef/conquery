package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ExecutionManager {

	/**
	 * @implNote DatasetRegistry serves as handle for {@link MetaStorage} which is loaded after {@link ExecutionManager}. Loading MetaStore however relies on setup and linked Namespace&Storage, which also contain an {@link ExecutionManager}.
	 */
	private final DatasetRegistry datasetRegistry;

	private final Cache<ManagedExecution<?>, List<List<EntityResult>>> executionResults = CacheBuilder.newBuilder()
																									  .softValues()
																									  .removalListener(this::executionRemoved)
																									  .build();

	/**
	 * Manage state of evicted Queries, setting them to NEW.
	 */
	private void executionRemoved(RemovalNotification<ManagedExecution<?>, List<?>> removalNotification) {

		// If removal was done manually we assume it was also handled properly
		if (!removalNotification.wasEvicted()) {
			return;
		}

		final ManagedExecution<?> execution = removalNotification.getKey();

		log.warn("Evicted Results for Query[{}] (Reason: {})", execution.getId(), removalNotification.getCause());

		execution.reset();
	}


	public ManagedExecution<?> runQuery(DatasetRegistry datasets, QueryDescription query, User user, Dataset submittedDataset, ConqueryConfig config, boolean system) {
		final ManagedExecution<?> execution = createExecution(datasets, query, user, submittedDataset, system);
		execute(datasets, execution, config);

		return execution;
	}

	public void execute(DatasetRegistry datasets, ManagedExecution<?> execution, ConqueryConfig config) {
		// Initialize the query / create subqueries
		try {
			execution.initExecutable(datasets, config);
		}
		catch (Exception e) {
			log.error("Failed to initialize Query[{}]", execution.getId(), e);

			//TODO we don't want to store completely faulty queries but is that right like this?
			datasets.getMetaStorage().removeExecution(execution.getId());
			throw e;
		}

		log.info("Executing Query[{}] in Datasets[{}]", execution.getQueryId(), execution.getRequiredDatasets());

		execution.start();

		final MetaStorage storage = datasets.getMetaStorage();
		final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(execution.getOwner(), storage).map(Group::getName).orElse("none");
		ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).inc();

		for (Namespace namespace : execution.getRequiredDatasets()) {
			namespace.sendToAll(execution.createExecutionMessage());
		}
	}

	public ManagedExecution<?> createExecution(DatasetRegistry datasets, QueryDescription query, User user, Dataset submittedDataset, boolean system) {
		return createQuery(datasets, query, UUID.randomUUID(), user, submittedDataset, system);
	}


	public ManagedExecution<?> createQuery(DatasetRegistry datasets, QueryDescription query, UUID queryId, User user, Dataset submittedDataset, boolean system) {
		// Transform the submitted query into an initialized execution
		ManagedExecution<?> managed = query.toManagedExecution(user, submittedDataset);
		managed.setSystem(system);
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

		MetaStorage metaStorage = datasetRegistry.getMetaStorage();

		final E query = (E) metaStorage.getExecution(result.getQueryId());

		if (query.getState() != ExecutionState.RUNNING) {
			return;
		}

		query.addResult(metaStorage, result);

		// State changed to DONE or FAILED
		if (query.getState() != ExecutionState.RUNNING) {
			final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(query.getOwner(), metaStorage).map(Group::getName).orElse("none");

			ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).dec();
			ExecutionMetrics.getQueryStateCounter(query.getState(), primaryGroupName).inc();
			ExecutionMetrics.getQueriesTimeHistogram(primaryGroupName).update(query.getExecutionTime().toMillis());
		}
	}


	/**
	 * Register another result for the execution.
	 */
	@SneakyThrows(ExecutionException.class) // can only occur if ArrayList::new fails which is unlikely and would have other problems also
	public void addQueryResult(ManagedExecution<?> execution, List<EntityResult> queryResults) {
		// We don't collect all results together into a fat list as that would cause lots of huge re-allocations for little gain.
		executionResults.get(execution, ArrayList::new)
						.add(queryResults);
	}

	/**
	 * Discard the query's results.
	 */
	public void clearQueryResults(ManagedExecution<?> execution) {
		executionResults.invalidate(execution);
	}

	/**
	 * Stream the results of the query, if available.
	 */
	public Stream<EntityResult> streamQueryResults(ManagedExecution<?> execution) {
		final List<List<EntityResult>> resultParts = executionResults.getIfPresent(execution);

		return resultParts == null
			   ? Stream.empty()
			   : resultParts.stream().flatMap(List::stream);
	}
}
