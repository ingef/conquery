package com.bakdata.conquery.models.query;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.google.common.cache.RemovalNotification;

public interface ExecutionManager {


	ManagedExecution runQuery(Namespace namespace, QueryDescription query, User user, Dataset submittedDataset, ConqueryConfig config, boolean system);

	void execute(Namespace namespace, ManagedExecution execution, ConqueryConfig config);

	ManagedExecution createExecution(QueryDescription query, User user, Dataset submittedDataset, boolean system);

	void cancelQuery(final Dataset dataset, final ManagedExecution query);

	/**
	 * Discard the query's results.
	 */
	void clearQueryResults(ManagedExecution execution);

	/**
	 * Stream the results of the query, if available.
	 */
	Stream<EntityResult> streamQueryResults(ManagedExecution execution);

}
