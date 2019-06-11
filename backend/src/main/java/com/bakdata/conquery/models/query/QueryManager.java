package com.bakdata.conquery.models.query;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteQuery;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerInformation;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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

	public ManagedQuery createQuery(IQuery query, User user) throws JSONException {
		return createQuery(query, UUID.randomUUID(), user);
	}
	
	public ManagedQuery createQuery(IQuery query, UUID queryId, User user) throws JSONException {
		query = query.resolve(new QueryResolveContext(
			namespace.getStorage().getMetaStorage(),
			namespace
		));
		ManagedQuery managed = new ManagedQuery(query, namespace, user.getId());
		managed.setQueryId(queryId);
		namespace.getStorage().getMetaStorage().addExecution(managed);
		queries.put(managed.getId(), managed);

		
		for(WorkerInformation worker : namespace.getWorkers()) {
			worker.send(new ExecuteQuery(managed));
		}
		return managed;
	}
	
	public ManagedQuery reexecuteQuery(ManagedQuery query) throws JSONException {
		query.initExecutable(namespace);
		queries.put(query.getId(), query);
		
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
