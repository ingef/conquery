package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.messages.namespaces.specific.CancelQuery;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DistributedExecutionManager extends ExecutionManager<DistributedExecutionManager.DistributedResult> {

	public record DistributedResult(List<List<EntityResult>> results) implements Result {

		public DistributedResult() {
			this(new ArrayList<>());
		}

		@Override
		public Stream<EntityResult> streamQueryResults() {
			return results.stream().flatMap(Collection::stream);
		}
	}

	private final ClusterState clusterState;


	public DistributedExecutionManager(MetaStorage storage, ClusterState state) {
		super(storage);
		clusterState = state;
	}


	@Override
	protected void doExecute(Namespace namespace, InternalExecution internalExecution) {
		final ManagedExecution execution = (ManagedExecution & InternalExecution<?>) internalExecution;

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
			return;
		}

		if (result.getError().isPresent()) {
			query.fail(result.getError().get());
		}
		else {

			// We don't collect all results together into a fat list as that would cause lots of huge re-allocations for little gain.
			final DistributedResult results = getResult(query, DistributedResult::new);
			results.results.add(result.getResults());

		}
	}

	@Override
	public void cancelQuery(Dataset dataset, ManagedExecution query) {
		log.debug("Sending cancel message to all workers.");

		getWorkerHandler(query).sendToAll(new CancelQuery(query.getId()));
	}

}
