package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteQuery;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class QueryManager {

	@NonNull
	private final Namespace namespace;

	public void initMaintenance(ScheduledExecutorService service) {
		if (service == null) {
			return;
		}

		service.scheduleAtFixedRate(
				this::maintain,
				0,
				1,
				TimeUnit.MINUTES
		);
	}

	public void maintain() {
		LocalDateTime threshold = LocalDateTime.now().minus(10L, ChronoUnit.MINUTES);
		// TODO: 11.10.2019 Maintain what and how ?
		//		for (ManagedExecution mq : queries.values()) {
		//			if (mq.getFinishTime() != null && mq.getFinishTime().isBefore(threshold)) {
		//				queries.remove(mq.getId());
		//			}
		//		}
	}

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
		//		queries.put(managed.getId(), managed);
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
		query.setState(ExecutionState.RUNNING);
		query.setStartTime(LocalDateTime.now());

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
		getQuery(result.getQueryId()).addResult(result);
	}

	public ManagedQuery getQuery(ManagedExecutionId id) {
		return (ManagedQuery) Objects.requireNonNull(namespace.getStorage().getMetaStorage().getExecution(id),"Unable to find query " + id.toString());
	}

}
