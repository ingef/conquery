package com.bakdata.conquery.models.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.CancelQuery;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteForm;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteQuery;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.WorkerHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class DistributedExecutionManager extends ExecutionManager {

	public record DistributedState(Map<WorkerId, List<EntityResult>> results, CountDownLatch executingLock) implements InternalState {

		@Override
		public Stream<EntityResult> streamQueryResults() {
			return results.values().stream().flatMap(Collection::stream);
		}

		@Override
		public CountDownLatch getExecutingLock() {
			return executingLock;
		}

		public boolean allResultsArrived(Set<WorkerId> allWorkers) {
			Set<WorkerId> finishedWorkers = results.keySet();
			return finishedWorkers.equals(allWorkers);
		}
	}

	private final ClusterState clusterState;


	public DistributedExecutionManager(MetaStorage storage, ClusterState state) {
		super(storage);
		clusterState = state;
	}


	@Override
	protected <E extends ManagedExecution & InternalExecution> void doExecute(E execution) {

		log.info("Executing Query[{}] in Dataset[{}]", execution.getQueryId(), execution.getDataset());

		addState(execution.getId(), new DistributedState(new ConcurrentHashMap<>(), new CountDownLatch(1)));

		if (execution instanceof ManagedInternalForm<?> form) {
			form.getSubQueries().values().forEach((query) -> addState(query.getId(), new DistributedState(new ConcurrentHashMap<>(), new CountDownLatch(1))));
		}

		final WorkerHandler workerHandler = getWorkerHandler(execution.getId().getDataset());

		workerHandler.sendToAll(createExecutionMessage(execution));
	}

	private WorkerMessage createExecutionMessage(ManagedExecution execution) {
		if (execution instanceof ManagedQuery mq) {
			return new ExecuteQuery(mq.getId(), mq.getQuery());
		}
		else if (execution instanceof ManagedInternalForm<?> form) {
			return new ExecuteForm(form.getId(), form.getFlatSubQueries()
													 .entrySet()
													 .stream()
													 .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getQuery())));
		}
		throw new NotImplementedException("Unable to build execution message for " + execution.getClass());

	}

	private WorkerHandler getWorkerHandler(DatasetId datasetId) {
		return clusterState.getWorkerHandlers().get(datasetId);
	}

	/**
	 * Receive part of query result and store into query.
	 *
	 * @implNote subQueries of Forms are managed by the form itself, so need to be passed from outside.
	 */
	@SneakyThrows
	public <R extends ShardResult, E extends ManagedExecution & InternalExecution> void handleQueryResult(R result, E execution) {


		log.debug("Received Result[size={}] for Query[{}]", result.getResults().size(), result.getQueryId());
		log.trace("Received Result\n{}", result.getResults());

		if (execution.getState() != ExecutionState.RUNNING) {
			log.warn("Received result for Query[{}] that is not RUNNING but {}", execution.getId(), execution.getState());
			return;
		}

		if (result.getError().isPresent()) {
			execution.fail(result.getError().get(), this);
		}
		else {

			// We don't collect all results together into a fat list as that would cause lots of huge re-allocations for little gain.
			State state = getResult(execution.getId());
			if (!(state instanceof DistributedState distributedState)) {
				throw new IllegalStateException("Expected execution '%s' to be of type %s, but was %s".formatted(execution.getId(), DistributedState.class, state.getClass()));
			}
			distributedState.results.put(result.getWorkerId(), result.getResults());

			// If all known workers have returned a result, the query is DONE.
			if (distributedState.allResultsArrived(getWorkerHandler(execution.getDataset().getId()).getAllWorkerIds())) {
				execution.finish(ExecutionState.DONE, this);

			}
		}

		// State changed to DONE or FAILED
		if (execution.getState() != ExecutionState.RUNNING) {
			final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(execution.getOwner(), getStorage()).map(Group::getName).orElse("none");

			ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).dec();
			ExecutionMetrics.getQueryStateCounter(execution.getState(), primaryGroupName).inc();
			ExecutionMetrics.getQueriesTimeHistogram(primaryGroupName).update(execution.getExecutionTime().toMillis());

			/* This log is here to prevent an NPE which could occur when no strong reference to result.getResults()
			 existed anymore after the query finished and immediately was reset */
			log.trace("Collected metrics for execution {}. Last result received: {}:", result.getQueryId(), result.getResults());
		}

	}

	@Override
	public void doCancelQuery(ManagedExecution execution) {
		log.debug("Sending cancel message to all workers.");

		execution.cancel();
		getWorkerHandler(execution.createId().getDataset()).sendToAll(new CancelQuery(execution.getId()));
	}

}
