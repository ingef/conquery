package com.bakdata.conquery.sql.conquery;


import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.sql.SqlContext;
import com.bakdata.conquery.sql.conversion.SqlConverter;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.execution.SqlExecutionResult;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import lombok.extern.slf4j.Slf4j;

//TODO FK: This class is nearly identical with DistributedExecutionManager => move shared code to super-class
@Slf4j
public class SqlExecutionManager implements ExecutionManager {
	private final MetaStorage storage;
	private final SqlExecutionService executionService;
	private final SqlConverter converter;

	private final Cache<ManagedExecutionId, SqlExecutionResult> executionResults =
			CacheBuilder.newBuilder()
						.softValues()
						.removalListener(this::executionRemoved)
						.build();

	public SqlExecutionManager(final SqlContext context, SqlExecutionService sqlExecutionService, MetaStorage storage) {
		this.storage = storage;
		executionService = sqlExecutionService;
		converter = new SqlConverter(context.getSqlDialect(), context.getConfig());
	}

	/**
	 * Manage state of evicted Queries, setting them to NEW.
	 */
	private void executionRemoved(RemovalNotification<ManagedExecutionId, ?> removalNotification) {
		// If removal was done manually we assume it was also handled properly
		if (!removalNotification.wasEvicted()) {
			return;
		}

		final ManagedExecutionId executionId = removalNotification.getKey();

		log.warn("Evicted Results for Query[{}] (Reason: {})", executionId, removalNotification.getCause());

		final ManagedExecution execution = storage.getExecution(executionId);

		// The query might already be deleted
		if (execution != null) {
			execution.reset();
		}
	}

	@Override
	public ManagedQuery runQuery(Namespace namespace, QueryDescription query, User user, Dataset submittedDataset, ConqueryConfig config, boolean system) {
		// required for properly setting date aggregation action in all nodes of the query graph
		query.resolve(new QueryResolveContext(namespace, config, storage, null));
		final ManagedQuery execution = createExecution(query, user, submittedDataset, system);

		execute(namespace, execution, config);

		return execution;
	}

	@Override
	public ManagedQuery createExecution(QueryDescription query, User user, Dataset submittedDataset, boolean system) {
		final Query castQuery = (Query) query;
		final ManagedQuery sqlManagedQuery = new ManagedQuery(castQuery, user, submittedDataset, storage);
		storage.addExecution(sqlManagedQuery);
		return sqlManagedQuery;
	}

	@Override
	public void execute(Namespace namespace, ManagedExecution execution, ConqueryConfig config) {
		if (!(execution instanceof ManagedQuery managedQuery)) {
			throw new UnsupportedOperationException("The SQL execution manager can only execute SQL queries, but got a %s".formatted(execution.getClass()));
		}

		execution.initExecutable(namespace, config);
		execution.start();

		// todo(tm): Non-blocking execution
		final SqlQuery sqlQuery = converter.convert(managedQuery.getQuery());

		final SqlExecutionResult result = executionService.execute(sqlQuery);
		executionResults.put(execution.getId(), result);
		managedQuery.setLastResultCount(((long) result.getRowCount()));

		execution.finish(ExecutionState.DONE);

	}

	@Override
	public void cancelQuery(Dataset dataset, ManagedExecution query) {
		// unsupported for now
	}

	@Override
	public void clearQueryResults(ManagedExecution execution) {
		executionResults.invalidate(execution.getId());
	}

	@Override
	public Stream<EntityResult> streamQueryResults(ManagedExecution execution) {
		return executionResults.getIfPresent(execution.getId()).getTable().stream();
	}

}
