package com.bakdata.conquery.models.query;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

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
		service.scheduleAtFixedRate(
			this::maintain,
			0,
			1,
			TimeUnit.MINUTES
		);
	}
	
	public void maintain() {
		LocalDateTime threshhold = LocalDateTime.now().minus(10L, ChronoUnit.MINUTES);
		
		for(ManagedQuery mq : queries.values()) {
			if(mq.getFinishTime() != null && mq.getFinishTime().isBefore(threshhold)) {
				queries.remove(mq.getId());
			}
		}
	}

	public ManagedQuery createQuery(IQuery query) throws JSONException {
		ManagedQuery managed = new ManagedQuery(query, namespace);
		namespace.getStorage().getMetaStorage().addQuery(managed);
		queries.add(managed);
		
		Map<ManagedQueryId, IQuery> requirements = managed
			.getQuery()
			.collectRequiredQueries()
			.stream()
			.collect(Collectors.toUnmodifiableMap(
				Function.identity(),
				id->namespace.getStorage().getMetaStorage().getQuery(id).getQuery()
			));
		
		for(WorkerInformation worker : namespace.getWorkers()) {
			worker.send(new ExecuteQuery(managed, requirements));
		}
		return managed;
	}

	public void addQueryResult(ShardResult result) {
		ManagedQuery managedQuery = queries.getOrFail(result.getQueryId());
		managedQuery.addResult(result);
	}

}
