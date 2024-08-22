package com.bakdata.conquery.models.query;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.result.ExternalState;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ExternalExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public abstract class ExecutionManager<R extends ExecutionManager.InternalState> {

	/**
	 * Holds all informations about an execution, which cannot/should not be serialized/cached in a store.
	 */
	public interface State {

		/**
		 * Synchronization barrier for web requests.
		 * Barrier is activated upon starting an execution so request can wait for execution completion.
		 * When the execution is finished the barrier is removed.
		 */
		CountDownLatch getExecutingLock();
	}

	public interface InternalState extends State{
		Stream<EntityResult> streamQueryResults();
	}

	private final MetaStorage storage;

	/**
	 * Cache for internal execution state.
	 */
	private final Cache<ManagedExecutionId, R> internalExecutionStates =
			CacheBuilder.newBuilder()
						.softValues()
						.removalListener(this::executionRemoved)
						.build();

	/**
	 * Cache for external execution state.
	 */
	private final Cache<ManagedExecutionId, ExternalState> externalExecutionStates =
			CacheBuilder.newBuilder()
						.softValues()
						.removalListener(this::executionRemoved)
						.build();

	/**
	 * Manage state of evicted Queries, setting them to NEW.
	 */
	private void executionRemoved(RemovalNotification<ManagedExecutionId, State> removalNotification) {
		// If removal was done manually we assume it was also handled properly
		if (!removalNotification.wasEvicted()) {
			return;
		}

		final ManagedExecutionId executionId = removalNotification.getKey();

		log.warn("Evicted Results for Query[{}] (Reason: {})", executionId, removalNotification.getCause());

		final ManagedExecution execution = getExecution(executionId);

		// The query might already be deleted
		if (execution != null) {
			execution.reset(this);
		}
	}


	public ManagedExecution getExecution(ManagedExecutionId execution) {
		return storage.getExecution(execution);
	}

	protected R getResult(ManagedExecutionId id) throws ExecutionException {
		return internalExecutionStates.getIfPresent(id);
	}

	public ExternalState getExternalResult(ManagedExecutionId id) {
		return externalExecutionStates.getIfPresent(id);
	}

	protected void addState(ManagedExecutionId id, R result) {
		internalExecutionStates.put(id, result);
	}

	/**
	 * Is called upon start by the external execution
	 */
	public void addState(ManagedExecutionId execution, ExternalState result) {
		externalExecutionStates.put(execution, result);
	}

	public final ManagedExecution runQuery(Namespace namespace, QueryDescription query, User user, ConqueryConfig config, boolean system) {
		final ManagedExecution execution = createExecution(query, user, namespace, system);

		execute(namespace, execution, config);

		return execution;
	}


	public final void execute(Namespace namespace, ManagedExecution execution, ConqueryConfig config) {

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

		ManagedExecutionId executionId = execution.getId();
		log.info("Starting execution[{}]", executionId);
		try {
			execution.start();

			final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(execution.getOwner(), storage).map(Group::getName).orElse("none");
			ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).inc();

			if (execution instanceof InternalExecution internalExecution) {
				doExecute((ManagedExecution & InternalExecution) internalExecution);
			}
		}
		catch (Exception e) {
			log.warn("Failed to execute '{}'", executionId);
			execution.fail(ConqueryError.asConqueryError(e), this);
		}
	}

	protected abstract <E extends ManagedExecution & InternalExecution> void doExecute(E execution);

	// Visible for testing
	public final ManagedExecution createExecution(QueryDescription query, User user, Namespace namespace, boolean system) {
		return createExecution(query, UUID.randomUUID(), user, namespace, system);
	}

	public final ManagedExecution createExecution(QueryDescription query, UUID queryId, User user, Namespace namespace, boolean system) {
		// Transform the submitted query into an initialized execution
		ManagedExecution managed = query.toManagedExecution(user, namespace.getDataset(), storage);
		managed.setSystem(system);
		managed.setQueryId(queryId);
		managed.setMetaStorage(storage);

		// Store the execution
		storage.addExecution(managed);

		return managed;
	}

	public final void cancelQuery(final ManagedExecution execution) {
		if (execution instanceof ExternalExecution externalExecution) {
			externalExecution.cancel();
			externalExecutionStates.invalidate(execution.getId());
			return;
		}

		doCancelQuery(execution);
	}


	public abstract void doCancelQuery(final ManagedExecution execution);

	public void clearQueryResults(ManagedExecution execution) {
		if (execution instanceof InternalExecution) {
			internalExecutionStates.invalidate(execution.getId());
			return;
		}
		externalExecutionStates.invalidate(execution.getId());
	}

	public <E extends ManagedExecution & InternalExecution> Stream<EntityResult> streamQueryResults(E execution) {
		final R resultParts = internalExecutionStates.getIfPresent(execution.getId());

		return resultParts == null
			   ? Stream.empty()
			   : resultParts.streamQueryResults();

	}

	public <E extends ManagedExecution & InternalExecution> void clearBarrierInternalExecution(E execution) {
		R result = Objects.requireNonNull(internalExecutionStates.getIfPresent(execution.getId()), "Cannot clear lock on absent execution result");

		result.getExecutingLock().countDown();
	}

	public void clearBarrierExternalExecution(ManagedExecutionId id) {
		ExternalState result = Objects.requireNonNull(externalExecutionStates.getIfPresent(id), "Cannot clear lock on absent execution result");

		result.getExecutingLock().countDown();
	}

	/**
	 * Blocks until an execution finished of the specified timeout is reached. Return immediately if the execution is not running
	 */
	public ExecutionState awaitDone(ManagedExecution execution, int time, TimeUnit unit) {
		ManagedExecutionId id = execution.getId();
		ExecutionState state = execution.getState();
		if (state != ExecutionState.RUNNING) {
			return state;
		}

		State result;
		if (execution instanceof InternalExecution) {
			result = internalExecutionStates.getIfPresent(id);
		}
		else {
			result = externalExecutionStates.getIfPresent(id);
		}

		if (result == null) {
			throw new IllegalStateException("Execution is running, but no result is registered");
		}
		Uninterruptibles.awaitUninterruptibly(result.getExecutingLock(), time, unit);

		return execution.getState();
	}
}
