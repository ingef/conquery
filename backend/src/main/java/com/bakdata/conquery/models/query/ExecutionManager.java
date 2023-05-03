package com.bakdata.conquery.models.query;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.google.common.cache.RemovalNotification;

public interface ExecutionManager {


	ManagedExecution runQuery(Namespace namespace, QueryDescription query, User user, Dataset submittedDataset, ConqueryConfig config, boolean system);

	void execute(Namespace namespace, ManagedExecution execution, ConqueryConfig config);

	public ManagedExecution createExecution(QueryDescription query, User user, Dataset submittedDataset, boolean system);

	public ManagedExecution createQuery(QueryDescription query, UUID queryId, User user, Dataset submittedDataset, boolean system);

	void cancelQuery(final Dataset dataset, final ManagedExecution query);

	/**
	 * Receive part of query result and store into query.
	 *
	 * @param result
	 */
	<R extends ShardResult, E extends ManagedExecution & InternalExecution<R>> void handleQueryResult(R result);

	/**
	 * Register another result for the execution.
	 */
	void addQueryResult(ManagedExecution execution, List<EntityResult> queryResults);

	void clearQueryResults(ManagedExecution execution);

	/**
	 * Stream the results of the query, if available.
	 */
	Stream<EntityResult> streamQueryResults(ManagedExecution execution);
}
