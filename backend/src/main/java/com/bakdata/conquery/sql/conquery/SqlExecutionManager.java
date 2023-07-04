package com.bakdata.conquery.sql.conquery;

import java.util.UUID;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.Namespace;

public class SqlExecutionManager implements ExecutionManager {
	@Override
	public ManagedExecution runQuery(Namespace namespace, QueryDescription query, User user, Dataset submittedDataset, ConqueryConfig config, boolean system) {
		return null;
	}

	@Override
	public void execute(Namespace namespace, ManagedExecution execution, ConqueryConfig config) {

	}

	@Override
	public ManagedExecution createExecution(QueryDescription query, User user, Dataset submittedDataset, boolean system) {
		return null;
	}

	@Override
	public void cancelQuery(Dataset dataset, ManagedExecution query) {

	}

	@Override
	public void clearQueryResults(ManagedExecution execution) {
	}

	@Override
	public Stream<EntityResult> streamQueryResults(ManagedExecution execution) {
		return null;
	}

}
