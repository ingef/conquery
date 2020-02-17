package com.bakdata.conquery.models.query;

import java.util.Objects;
import java.util.UUID;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteQuery;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.codahale.metrics.MetricRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryManager {

	@NonNull
	private final Namespace namespace;
	private final MetricRegistry metricRegistry;

	public ManagedQuery runQuery(IQuery query, User user) throws JSONException {
		return runQuery(query, UUID.randomUUID(), user);
	}

	public ManagedQuery runQuery(IQuery query, UUID queryId, User user) throws JSONException {
		return executeQuery(createQuery(query, queryId, user));
	}

	public ManagedQuery createQuery(IQuery query, UUID queryId, User user) throws JSONException {
		query = query.resolve(new QueryResolveContext(
				namespace.getStorage().getMetaStorage(),
				namespace
		));

		ManagedQuery managed = new ManagedQuery(query, namespace, user.getId());
		managed.setQueryId(queryId);
		namespace.getStorage().getMetaStorage().addExecution(managed);

		return managed;
	}

	/**
	 * Send message for query execution to all workers.
	 *
	 * @param query
	 * @return
	 */
	public ManagedQuery executeQuery(ManagedQuery query) {

		query.initExecutable(namespace);
		query.start();

		for(WorkerInformation worker : namespace.getWorkers()) {
			worker.send(new ExecuteQuery(query));
		}
		return query;
	}

	/**
	 * Receive part of query result and store into query.
	 * @param result
	 */
	public void addQueryResult(ShardResult result) {
		final ManagedQuery query = getQuery(result.getQueryId());
		query.addResult(result);

		if (query.getState() != ExecutionState.RUNNING) {
			metricRegistry.counter("queries.state." + query.getState()).inc();
			metricRegistry.histogram("queries.time").update(query.getExecutionTime().toMillis());
		}
	}

	public ManagedQuery getQuery(ManagedExecutionId id) {
		return (ManagedQuery) Objects.requireNonNull(namespace.getStorage().getMetaStorage().getExecution(id),"Unable to find query " + id.toString());
	}

}
