package com.bakdata.conquery.sql.conquery;


import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.sql.SqlContext;
import com.bakdata.conquery.sql.conversion.SqlConverter;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.execution.SqlExecutionResult;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlExecutionManager implements ExecutionManager {
	private final MetaStorage metaStorage;
	private final SqlExecutionService executionService;
	private final SqlConverter converter;

	public SqlExecutionManager(final SqlContext context, SqlExecutionService sqlExecutionService, MetaStorage metaStorage) {
		SqlDialect sqlDialect = context.getSqlDialect();
		this.metaStorage = metaStorage;
		this.executionService = sqlExecutionService;
		this.converter = new SqlConverter(sqlDialect, context.getConfig());
	}

	@Override
	public SqlManagedQuery runQuery(Namespace namespace, QueryDescription query, User user, Dataset submittedDataset, ConqueryConfig config, boolean system) {
		// required for properly setting date aggregation action in all nodes of the query graph
		query.resolve(new QueryResolveContext(namespace, config, metaStorage, null));
		SqlManagedQuery execution = createExecution(query, user, submittedDataset, system);
		execution.initExecutable(namespace, config);
		execution.start();
		// todo(tm): Non-blocking execution
		SqlExecutionResult result = this.executionService.execute(execution);
		execution.finish(result);
		return execution;
	}

	@Override
	public void execute(Namespace namespace, ManagedExecution execution, ConqueryConfig config) {
		if (!(execution instanceof SqlManagedQuery)) {
			throw new UnsupportedOperationException("The SQL execution manager can only execute SQL queries, but got a %s".formatted(execution.getClass()));
		}

		this.executionService.execute(((SqlManagedQuery) execution));
	}

	@Override
	public SqlManagedQuery createExecution(QueryDescription query, User user, Dataset submittedDataset, boolean system) {
		Query castQuery = (Query) query;
		SqlQuery converted = this.converter.convert(castQuery);
		SqlManagedQuery sqlManagedQuery = new SqlManagedQuery(castQuery, user, submittedDataset, metaStorage, converted);
		metaStorage.addExecution(sqlManagedQuery);
		return sqlManagedQuery;
	}

	@Override
	public void cancelQuery(Dataset dataset, ManagedExecution query) {
		// unsupported for now
	}

	@Override
	public void clearQueryResults(ManagedExecution execution) {
		// unsupported for now
	}

	@Override
	public Stream<EntityResult> streamQueryResults(ManagedExecution execution) {
		throw new UnsupportedOperationException("Streaming for now not supported");
	}

}
