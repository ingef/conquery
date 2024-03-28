package com.bakdata.conquery.models.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.specific.CancelQuery;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DistributedExecutionManager extends ExecutionManager<DistributedExecutionManager.DistributedResult> {

	public record DistributedResult(Map<WorkerId, List<EntityResult>> results) implements Result {

		public DistributedResult() {
			this(new ConcurrentHashMap<>());
		}

		@Override
		public Stream<EntityResult> streamQueryResults() {
			return results.values().stream().flatMap(Collection::stream);
		}
	}

	private final ClusterState clusterState;


	public DistributedExecutionManager(MetaStorage storage, ClusterState state) {
		super(storage);
		clusterState = state;
	}


	@Override
	protected void doExecute(Namespace namespace, InternalExecution internalExecution) {
		ManagedExecution execution = (ManagedExecution & InternalExecution<?>) internalExecution;

		log.info("Executing Query[{}] in Dataset[{}]", execution.getQueryId(), namespace.getDataset().getId());

		final WorkerHandler workerHandler = getWorkerHandler(execution);

		workerHandler.sendToAll(internalExecution.createExecutionMessage());
	}

	private WorkerHandler getWorkerHandler(ManagedExecution execution) {
		return clusterState.getWorkerHandlers()
						   .get(execution.getDataset().getId());
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
			log.warn("Received result for Query[{}] that is not RUNNING but {}", query.getId(), query.getState());
			return;
		}

		if (result.getError().isPresent()) {
			query.fail(result.getError().get());
		}
		else {

			// We don't collect all results together into a fat list as that would cause lots of huge re-allocations for little gain.
			final DistributedResult results = getResult(query, DistributedResult::new);
			results.results.put(result.getWorkerId(), result.getResults());

			final Set<WorkerId> finishedWorkers = results.results.keySet();

			// If all known workers have returned a result, the query is DONE.
			if (finishedWorkers.equals(getWorkerHandler(query).getAllWorkerIds())) {
				query.finish(ExecutionState.DONE);
			}
		}

		// State changed to DONE or FAILED
		if (query.getState() != ExecutionState.RUNNING) {
			final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(query.getOwner(), getStorage()).map(Group::getName).orElse("none");

			ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).dec();
			ExecutionMetrics.getQueryStateCounter(query.getState(), primaryGroupName).inc();
			ExecutionMetrics.getQueriesTimeHistogram(primaryGroupName).update(query.getExecutionTime().toMillis());

			/* This log is here to prevent an NPE which could occur when no strong reference to result.getResults()
			 existed anymore after the query finished and immediately was reset */
			log.trace("Collected metrics for execution {}. Last result received: {}:", result.getQueryId(), result.getResults());
		}

	}

	@Override
	public void cancelQuery(Dataset dataset, ManagedExecution query) {
		log.debug("Sending cancel message to all workers.");

		query.cancel();
		getWorkerHandler(query).sendToAll(new CancelQuery(query.getId()));
	}

}
