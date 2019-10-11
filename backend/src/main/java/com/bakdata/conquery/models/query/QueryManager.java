package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteQuery;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerInformation;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class QueryManager {

	@NonNull
	private final Namespace namespace;
	private final Map<ManagedExecutionId, ManagedQuery> queries = new HashMap<>();

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

		for (ManagedExecution mq : queries.values()) {
			if (mq.getFinishTime() != null && mq.getFinishTime().isBefore(threshold)) {
				queries.remove(mq.getId());
			}
		}
	}

	public ManagedQuery runQuery(IQuery query, User user) throws JSONException {
		return runQuery(query, UUID.randomUUID(), user);
	}
	
	public ManagedQuery runQuery(IQuery query, UUID queryId, User user) throws JSONException {
		return executeQuery(createQuery(query, queryId, user));
	}

	public ManagedQuery createQuery(IQuery query, UUID queryId, User user) {
		query = query.resolve(new QueryResolveContext(
			namespace.getStorage().getMetaStorage(),
			namespace
		));

		ManagedQuery managed = new ManagedQuery(query, namespace, user.getId());
		managed.setQueryId(queryId);
		queries.put(managed.getId(), managed);

		return managed;
	}

	private ManagedQuery executeQuery(ManagedQuery query) throws JSONException {
		namespace.getStorage().getMetaStorage().addExecution(query);

		query.initExecutable(namespace);
		query.setState(ExecutionState.RUNNING);
		query.setStartTime(LocalDateTime.now());

		for(WorkerInformation worker : namespace.getWorkers()) {
			worker.send(new ExecuteQuery(query));
		}
		return query;
	}

	public void addQueryResult(ShardResult result) {
		ManagedQuery managedQuery = getQuery(result.getQueryId());
		managedQuery.addResult(result);
	}

	public ManagedQuery getQuery(ManagedExecutionId id) {
		return Objects.requireNonNull(queries.get(id), ()->"unknown query id "+id);
	}

}
