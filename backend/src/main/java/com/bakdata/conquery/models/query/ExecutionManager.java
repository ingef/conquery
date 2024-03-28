package com.bakdata.conquery.models.query;

import java.util.UUID;
import java.util.concurrent.Callable;
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
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public abstract class ExecutionManager<R extends ExecutionManager.Result> {

	public interface Result {
		Stream<EntityResult> streamQueryResults();
	}

	private final MetaStorage storage;

	private final Cache<ManagedExecutionId, R> executionResults =
			CacheBuilder.newBuilder()
						.softValues()
						.removalListener(this::executionRemoved)
						.build();

	/**
	 * Manage state of evicted Queries, setting them to NEW.
	 */
	private void executionRemoved(RemovalNotification<ManagedExecutionId, R> removalNotification) {
		// If removal was done manually we assume it was also handled properly
		if (!removalNotification.wasEvicted()) {
			return;
		}

		final ManagedExecutionId executionId = removalNotification.getKey();

		log.warn("Evicted Results for Query[{}] (Reason: {})", executionId, removalNotification.getCause());

		final ManagedExecution execution = getExecution(executionId);

		// The query might already be deleted
		if (execution != null) {
			execution.reset();
		}
	}


	public ManagedExecution getExecution(ManagedExecutionId execution) {
		return storage.getExecution(execution);
	}

	protected R getResult(ManagedExecution execution, Callable<R> defaultProvider) throws ExecutionException {
		return executionResults.get(execution.getId(), defaultProvider);
	}

	protected void addResult(ManagedExecution execution, R result) {
		executionResults.put(execution.getId(), result);
	}

	public final  ManagedExecution runQuery(Namespace namespace, QueryDescription query, User user, Dataset submittedDataset, ConqueryConfig config, boolean system) {
		final ManagedExecution execution = createExecution(query, user, submittedDataset, system);
		execute(namespace, execution, config);

		return execution;
	}


	public final  void execute(Namespace namespace, ManagedExecution execution, ConqueryConfig config) {

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
			doExecute(namespace, internalExecution);
		}
	}

	protected abstract void doExecute(Namespace namespace, InternalExecution<?> execution);

	// Visible for testing
	public final ManagedExecution createExecution(QueryDescription query, User user, Dataset submittedDataset, boolean system) {
		return createQuery(query, UUID.randomUUID(), user, submittedDataset, system);
	}

	public final ManagedExecution createQuery(QueryDescription query, UUID queryId, User user, Dataset submittedDataset, boolean system) {
		// Transform the submitted query into an initialized execution
		ManagedExecution managed = query.toManagedExecution(user, submittedDataset, storage);
		managed.setSystem(system);
		managed.setQueryId(queryId);

		// Store the execution
		storage.addExecution(managed);

		return managed;
	}

	public abstract void cancelQuery(final Dataset dataset, final ManagedExecution query);

	public void clearQueryResults(ManagedExecution execution) {
		executionResults.invalidate(execution.getId());
	}

	public Stream<EntityResult> streamQueryResults(ManagedExecution execution) {
		final R resultParts = executionResults.getIfPresent(execution.getId());

		return resultParts == null
			   ? Stream.empty()
			   : resultParts.streamQueryResults();

	}
}
