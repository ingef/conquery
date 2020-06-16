package com.bakdata.conquery.models.query;

import java.util.Objects;
import java.util.UUID;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteQuery;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ExecutionManager {

	@NonNull
	private final Namespace namespace;

	public static ManagedExecution<?> runQuery(Namespaces namespaces, QueryDescription query, UserId userId, DatasetId submittedDataset) {
		return execute(namespaces, createExecution(namespaces, query, userId, submittedDataset));
	}
	
	public static ManagedExecution<?> runQuery(Namespaces namespaces, QueryDescription query, UUID queryId, UserId userId, DatasetId submittedDataset) {
		return execute(namespaces, createQuery(namespaces, query, queryId, userId, submittedDataset));
	}
	

	public static ManagedExecution<?> createExecution(Namespaces namespaces, QueryDescription query, UserId userId, DatasetId submittedDataset) {
		return createQuery( namespaces, query, UUID.randomUUID(), userId, submittedDataset);
	}

	public static ManagedExecution<?> createQuery(Namespaces namespaces, QueryDescription query, UUID queryId, UserId userId, DatasetId submittedDataset) {
		// Transform the submitted query into an initialized execution
		ManagedExecution<?> managed = query.toManagedExecution( namespaces, userId, submittedDataset);

		managed.setQueryId(queryId);
		
		// Store the execution
		namespaces.getMetaStorage().addExecution(managed);

		return managed;
	}

	public static ManagedExecution<?> execute(Namespaces namespaces, ManagedExecution<?> execution){
		log.info("Executing Query[{}] in Namesspaces[{}]", execution.getQueryId(), execution.getRequiredNamespaces());
		// Initialize the query / create subqueries
		execution.initExecutable(namespaces);

		execution.start();

		final MasterMetaStorage storage = namespaces.getMetaStorage();
		final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(storage.getUser(execution.getOwner()), storage).map(Group::getName).orElse("none");
		ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).inc();

		for(Namespace namespace : execution.getRequiredNamespaces()) {
			namespace.getQueryManager().executeQueryInNamespace(execution);
		}
		return execution;
	}

	/**
	 * Send message for query execution to all workers.
	 *
	 * @param query
	 * @return
	 */
	private ManagedExecution<?> executeQueryInNamespace(ManagedExecution<?> query) {
		log.trace("Sending Query[{}] to Workers[{}]", query.getQueryId(), workers);

		namespace.sendToAll(new ExecuteQuery(query));
		return query;
	}

	/**
	 * Receive part of query result and store into query.
	 * @param result
	 */
	public <R extends ShardResult, E extends ManagedExecution<R>> void addQueryResult(R result) {
		final MasterMetaStorage storage = namespace.getNamespaces().getMetaStorage();

		final E query = (E) getQuery(result.getQueryId());
		query.addResult(storage, result);

		if(query.getState() == ExecutionState.DONE || query.getState() == ExecutionState.FAILED){
			final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(storage.getUser(query.getOwner()), storage).map(Group::getName).orElse("none");

			ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).dec();
			ExecutionMetrics.getQueryStateCounter(query.getState(), primaryGroupName).inc();
			ExecutionMetrics.getQueriesTimeHistogram(primaryGroupName).update(query.getExecutionTime().toMillis());
		}
	}

	public ManagedExecution<?> getQuery(@NonNull ManagedExecutionId id) {
		return Objects.requireNonNull(namespace.getStorage().getMetaStorage().getExecution(id),"Unable to find query " + id.toString());
	}

}
