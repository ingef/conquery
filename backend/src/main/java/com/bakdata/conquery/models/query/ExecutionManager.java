package com.bakdata.conquery.models.query;

import java.util.Objects;
import java.util.UUID;

import com.bakdata.conquery.apiv1.SubmittedQuery;
import com.bakdata.conquery.models.auth.entities.User;
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
public class ExecutionManager {

	@NonNull
	private final Namespace namespace;

	public ManagedQuery runQuery(IQuery query, User user) throws JSONException {
		return runQuery(query, UUID.randomUUID(), user);
	}

	public ManagedQuery runQuery(IQuery query, UUID queryId, User user) throws JSONException {
		return executeQuery(createQuery(query, queryId, user));
	}

	public static ManagedExecution createQuery(SubmittedQuery query, UUID queryId, UserId userId) throws JSONException {
		ManagedExecution query.wrapInManagedType(storage, namespaces, userId, submittedDataset)


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
	public ManagedExecution executeQuery(ManagedExecution query) {

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
		getQuery(result.getQueryId()).addResult(result);
	}

	public ManagedExecution getQuery(ManagedExecutionId id) {
		return Objects.requireNonNull(namespace.getStorage().getMetaStorage().getExecution(id),"Unable to find query " + id.toString());
	}

}
