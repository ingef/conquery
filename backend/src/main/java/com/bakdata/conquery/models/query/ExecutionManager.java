package com.bakdata.conquery.models.query;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ExternalExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Data
@Slf4j
public abstract class ExecutionManager {

	private final MetaStorage storage;
	private final DatasetRegistry<?> datasetRegistry;

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
			execution.reset();
		}
	}

	public ManagedExecution getExecution(ManagedExecutionId execution) {
		return storage.getExecution(execution);
	}

	/**
	 * Returns the state or throws an NoSuchElementException if no state was found.
	 */
	public <R extends State> R getResult(ManagedExecutionId id) {
		State state = executionStates.getIfPresent(id);
		if (state == null) {
			throw new NoSuchElementException("No execution found for %s".formatted(id));
		}
		return (R) state;
	}	/**
	 * Cache for execution states.
	 */
	private final Cache<ManagedExecutionId, State> executionStates =
			CacheBuilder.newBuilder()
						.softValues()
						.removalListener(this::executionRemoved)
						.build();

	public <R extends State> Optional<R> tryGetResult(ManagedExecutionId id) {
		return Optional.ofNullable((R) executionStates.getIfPresent(id));
	}

	public boolean isResultPresent(ManagedExecutionId id) {
		return executionStates.getIfPresent(id) != null;
	}

	public void addState(ManagedExecutionId id, State result) {
		executionStates.put(id, result);
	}

	public final ManagedExecution runQuery(Namespace namespace, QueryDescription query, UserId user, ConqueryConfig config, boolean system) {
		final ManagedExecution execution = createExecution(query, user, namespace, system);

		execute(execution, config);

		return execution;
	}

	// Visible for testing
	public final ManagedExecution createExecution(QueryDescription query, UserId user, Namespace namespace, boolean system) {
		return createExecution(query, UUID.randomUUID(), user, namespace, system);
	}

	public final void execute(ManagedExecution execution, ConqueryConfig config) {

		clearQueryResults(execution);

		try {
			execution.initExecutable(config);
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

			final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(execution.getOwner().resolve(), storage).map(Group::getName).orElse("none");
			ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).inc();

			if (execution instanceof InternalExecution internalExecution) {
				doExecute((ManagedExecution & InternalExecution) internalExecution);
			}

		}
		catch (Exception e) {
			log.warn("Failed to execute '{}'", executionId);
			execution.fail(ConqueryError.asConqueryError(e));
		}
	}

	public final ManagedExecution createExecution(QueryDescription query, UUID queryId, UserId user, Namespace namespace, boolean system) {
		// Transform the submitted query into an initialized execution
		ManagedExecution managed = query.toManagedExecution(user, namespace.getDataset().getId(), storage, datasetRegistry);
		managed.setSystem(system);
		managed.setQueryId(queryId);
		managed.setMetaStorage(storage);

		// Store the execution
		storage.addExecution(managed);

		return managed;
	}

	public void clearQueryResults(ManagedExecution execution) {
		executionStates.invalidate(execution.getId());
	}

	protected abstract <E extends ManagedExecution & InternalExecution> void doExecute(E execution);

	public final void cancelQuery(final ManagedExecution execution) {
		executionStates.invalidate(execution.getId());

		if (execution instanceof ExternalExecution externalExecution) {
			externalExecution.cancel();
			return;
		}
		doCancelQuery(execution);
	}

	public abstract void doCancelQuery(final ManagedExecution execution);

	public void updateState(ManagedExecutionId id, ExecutionState execState) {
		State state = executionStates.getIfPresent(id);
		if (state != null) {
			state.setState(execState);
			return;
		}

		log.warn("Could not update execution state of {} to {}, because it had no state.", id, execState);
	}

	public <E extends ManagedExecution & InternalExecution> Stream<EntityResult> streamQueryResults(E execution) {
		final InternalState resultParts = (InternalState) executionStates.getIfPresent(execution.getId());

		return resultParts == null
			   ? Stream.empty()
			   : resultParts.streamQueryResults();

	}

	public void clearBarrier(ManagedExecutionId id) {
		State result = Objects.requireNonNull(executionStates.getIfPresent(id), "Cannot clear lock on absent execution result");

		result.getExecutingLock().countDown();
	}

	/**
	 * Blocks until an execution finished of the specified timeout is reached. Return immediately if the execution is not running
	 */
	public ExecutionState awaitDone(ManagedExecution execution, int time, TimeUnit unit) {
		ManagedExecutionId id = execution.getId();
		State state = executionStates.getIfPresent(id);
		if (state == null) {
			return ExecutionState.NEW;
		}
		ExecutionState execState = state.getState();
		if (execState != ExecutionState.RUNNING) {
			return execState;
		}

		State result = executionStates.getIfPresent(id);

		if (result == null) {
			throw new IllegalStateException("Execution is running, but no result is registered");
		}
		Uninterruptibles.awaitUninterruptibly(result.getExecutingLock(), time, unit);

		State stateAfterWait = executionStates.getIfPresent(id);
		if (stateAfterWait == null) {
			return ExecutionState.NEW;
		}
		return stateAfterWait.getState();
	}

	/**
	 * Holds all informations about an execution, which cannot/should not be serialized/cached in a store.
	 */
	public interface State {

		/**
		 * The current {@link ExecutionState} of the execution.
		 */
		@NotNull
		ExecutionState getState();

		void setState(ExecutionState state);

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


}
