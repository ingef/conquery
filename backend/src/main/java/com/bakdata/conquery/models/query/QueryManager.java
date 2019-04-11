package com.bakdata.conquery.models.query;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
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
	private final IdMap<ManagedQueryId, ManagedQuery> queries = new IdMap<>();

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

		for (ManagedQuery mq : queries.values()) {
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
		namespace.getStorage().getMetaStorage().addQuery(managed);
		queries.add(managed);

		
		for(WorkerInformation worker : namespace.getWorkers()) {
			worker.send(new ExecuteQuery(managed));
		}
		return managed;
	}
	
	public ManagedQuery reexecuteQuery(ManagedQuery query) throws JSONException {
		query.initExecutable(namespace);
		queries.add(query);
		
		for(WorkerInformation worker : namespace.getWorkers()) {
			worker.send(new ExecuteQuery(query));
		}
		return query;
	}

	public void addQueryResult(ShardResult result) {
		ManagedQuery managedQuery = queries.getOrFail(result.getQueryId());
		managedQuery.addResult(result);
	}

	public ManagedQuery getQuery(ManagedQueryId id) {
		return queries.getOrFail(id);
	}

}
