package com.bakdata.conquery.models.query;

import static com.bakdata.conquery.models.execution.ExecutionState.RUNNING;

import java.util.List;
import java.util.NoSuchElementException;
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
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Data
@Slf4j
public abstract class ExecutionManager {

	private final MetaStorage storage;
	private final DatasetRegistry<?> datasetRegistry;
	private final ConqueryConfig config;

	/**
	 * Implements a rudimentary L1/L2 caching scheme:
	 * * L2 is expected to be softValues, to be vacuumed up by the GC.
	 * * L1 should be optimally be hard cache, with timeout characteristics.
	 * <br />
	 * This ensures, new {@link ExecutionInfo} stay in memory for a certain duration, after which they are left to the GC.
	 * <br />
	 * One risk to note is, that too many large {@link ExecutionInfo} may overwhelm the manager, but that's a pretty unlikely scenario and more likely caused my faulty or malicious clients. If we want to avoid this, L1 spec should probably limit the number of executions.
	 */
	@Getter(AccessLevel.NONE)
	private final Cache<ManagedExecutionId, ExecutionInfo> executionInfosL1;
	@Getter(AccessLevel.NONE)
	private final Cache<ManagedExecutionId, ExecutionInfo> executionInfosL2;

	public ExecutionManager(MetaStorage storage, DatasetRegistry<?> datasetRegistry, ConqueryConfig config) {
		this.storage = storage;
		this.datasetRegistry = datasetRegistry;
		this.config = config;

		executionInfosL2 = Caffeine.from(config.getQueries().getL2CacheSpec()).removalListener(this::evictionL2).build();
		executionInfosL1 = Caffeine.from(config.getQueries().getL1CacheSpec()).removalListener(this::evictionL1).build();
	}

	/**
	 * Manage state of evicted Queries, setting them to NEW.
	 */
	private void evictionL2(ManagedExecutionId executionId, ExecutionInfo resultInfo, RemovalCause cause) {

		// If removal was done manually we assume it was also handled properly
		if (!cause.wasEvicted()) {
			return;
		}

		log.trace("Evicted Query[{}] results from L2 cache (Reason: {})", executionId, cause);
	}

	private void evictionL1(ManagedExecutionId executionId, ExecutionInfo resultInfo, RemovalCause cause) {
		// If removal was done manually we assume it was also handled properly
		if (!cause.wasEvicted()) {
			return;
		}

		log.trace("Evicted Query[{}] results from L1 cache (Reason: {})", executionId, cause);
	}

	public ManagedExecution getExecution(ManagedExecutionId execution) {
		return storage.getExecution(execution);
	}

	public void reset(ManagedExecutionId id) {
		// This avoids endless loops with already reset queries
		if (!isResultPresent(id)) {
			return;
		}

		clearQueryResults(id);
	}

	public boolean isResultPresent(ManagedExecutionId id) {
		return tryGetExecutionInfo(id).isPresent();
	}

	public void clearQueryResults(ManagedExecutionId execution) {
		executionInfosL2.invalidate(execution);
		executionInfosL1.invalidate(execution);
	}

	public <R extends ExecutionInfo> Optional<R> tryGetExecutionInfo(ManagedExecutionId id) {
		// Access L1 before L2, to keep things "touched"
		ExecutionInfo maybeInfo = executionInfosL1.getIfPresent(id);

		if (maybeInfo != null) {
			return Optional.of((R) maybeInfo);
		}

		maybeInfo = executionInfosL2.getIfPresent(id);

		if (maybeInfo != null) {
			return Optional.of((R) maybeInfo);
		}

		return Optional.empty();
	}

	public void addState(ManagedExecutionId id, ExecutionInfo result) {
		executionInfosL1.put(id, result);
		executionInfosL2.put(id, result);
	}

	public final ManagedExecution runQuery(Namespace namespace, QueryDescription query, UserId user, boolean system) {
		final ManagedExecution execution = createExecution(query, user, namespace, system);

		execute(execution);

		return execution;
	}

	// Visible for testing
	public final ManagedExecution createExecution(QueryDescription query, UserId user, Namespace namespace, boolean system) {
		return createExecution(query, UUID.randomUUID(), user, namespace, system);
	}

	public final void execute(ManagedExecution execution) {

		clearQueryResults(execution.getId());

		try {
			execution.initExecutable();
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

		log.info("Starting execution[{}]", execution.getId());
		try {

			execution.start();

			final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(execution.getOwner().resolve(), storage).map(Group::getName).orElse("none");
			ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).inc();

			if (execution instanceof InternalExecution internalExecution) {
				doExecute((ManagedExecution & InternalExecution) internalExecution);
			}

		}
		catch (Exception e) {
			log.warn("Failed to execute '{}'", execution.getId());
			execution.fail(ConqueryError.asConqueryError(e));
		}
	}

	public final ManagedExecution createExecution(QueryDescription query, UUID queryId, UserId user, Namespace namespace, boolean system) {
		// Transform the submitted query into an initialized execution
		ManagedExecution managed = query.toManagedExecution(user, namespace.getDataset().getId(), storage, datasetRegistry, getConfig());
		managed.setSystem(system);
		managed.setQueryId(queryId);

		// Store the execution
		storage.addExecution(managed);

		return managed;
	}

	protected abstract <E extends ManagedExecution & InternalExecution> void doExecute(E execution);

	public final void cancelExecution(final ManagedExecution execution) {

		if (execution instanceof ExternalExecution externalExecution) {
			externalExecution.cancel();
			return;
		}

		clearQueryResults(execution.getId());

		doCancelQuery(execution.getId());
	}

	public abstract void doCancelQuery(ManagedExecutionId managedExecutionId);

	public void updateState(ManagedExecutionId id, ExecutionState execState) {
		Optional<ExecutionInfo> executionInfo = tryGetExecutionInfo(id);

		executionInfo.ifPresentOrElse(info -> info.setExecutionState(execState),
									  () -> log.warn("Could not update execution executionInfo of {} to {}, because it had no executionInfo.", id, execState)
		);
	}

	public <E extends ManagedExecution & InternalExecution> Stream<EntityResult> streamQueryResults(E execution) {
		Optional<InternalExecutionInfo> maybeInfo = tryGetExecutionInfo(execution.getId());

		return maybeInfo.map(InternalExecutionInfo::streamQueryResults).orElseGet(Stream::empty);
	}

	public void clearBarrier(ManagedExecutionId id) {
		ExecutionInfo executionInfo = getExecutionInfo(id);

		executionInfo.getExecutingLock().countDown();
	}

	/**
	 * Returns the state or throws an NoSuchElementException if no state was found.
	 */
	public <R extends ExecutionInfo> R getExecutionInfo(ManagedExecutionId id) {
		Optional<R> maybeInfo = tryGetExecutionInfo(id);

		if (maybeInfo.isPresent()) {
			return maybeInfo.get();
		}

		throw new NoSuchElementException("Could not find Execution %s".formatted(id));
	}

	/**
	 * Blocks until an execution finished of the specified timeout is reached. Return immediately if the execution is not running
	 */
	public ExecutionState awaitDone(ManagedExecutionId id, int time, TimeUnit unit) {

		Optional<ExecutionInfo> maybeExecutionInfo = tryGetExecutionInfo(id);

		if (maybeExecutionInfo.isEmpty()) {
			return ExecutionState.NEW;
		}

		ExecutionInfo executionInfo = maybeExecutionInfo.get();
		ExecutionState execState = executionInfo.getExecutionState();

		if (execState != RUNNING) {
			return execState;
		}

		Uninterruptibles.awaitUninterruptibly(executionInfo.getExecutingLock(), time, unit);

		Optional<ExecutionInfo> maybeExecutionInfoAfterWait = tryGetExecutionInfo(id);
		return maybeExecutionInfoAfterWait.map(ExecutionInfo::getExecutionState).orElse(ExecutionState.NEW);
	}

	public boolean hasRunningQueries() {
		if (executionInfosL2.asMap().values().stream().map(ExecutionInfo::getExecutionState).anyMatch(RUNNING::equals)) {
			return true;
		}

		return executionInfosL1.asMap().values().stream().map(ExecutionInfo::getExecutionState).anyMatch(RUNNING::equals);
	}

	/**
	 * Holds all informations about an execution, which cannot/should not be serialized/cached in a store.
	 */
	public interface ExecutionInfo {

		/**
		 * The current {@link ExecutionState} of the execution.
		 */
		@NotNull ExecutionState getExecutionState();

		void setExecutionState(ExecutionState state);

		/**
		 * Synchronization barrier for web requests.
		 * Barrier is activated upon starting an execution so request can wait for execution completion.
		 * When the execution is finished the barrier is removed.
		 */
		CountDownLatch getExecutingLock();
	}

	public interface InternalExecutionInfo extends ExecutionInfo {
		Stream<EntityResult> streamQueryResults();
		List<ResultInfo> getResultInfos();
		long getResultCount();
	}


}
