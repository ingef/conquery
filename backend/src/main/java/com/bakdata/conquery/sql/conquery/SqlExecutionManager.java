package com.bakdata.conquery.sql.conquery;


import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.sql.conversion.SqlConverter;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.execution.SqlExecutionResult;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SqlExecutionManager extends ExecutionManager<SqlExecutionResult> {

	private final SqlExecutionService executionService;
	private final SqlConverter converter;
	private final Map<ManagedExecutionId, CompletableFuture<Void>> runningExecutions;

	public SqlExecutionManager(SqlConverter sqlConverter, SqlExecutionService sqlExecutionService, MetaStorage storage) {
		super(storage);
		this.converter = sqlConverter;
		this.executionService = sqlExecutionService;
		this.runningExecutions = new HashMap<>();
	}

	@Override
	protected void doExecute(Namespace namespace, InternalExecution<?> execution) {

		SqlExecutionManager executionManager = (SqlExecutionManager) namespace.getExecutionManager();

		if (execution instanceof ManagedQuery managedQuery) {
			CompletableFuture<Void> sqlQueryExecution = executeAsync(managedQuery, executionManager);
			runningExecutions.put(managedQuery.getId(), sqlQueryExecution);
			return;
		}

		if (execution instanceof ManagedInternalForm<?> managedForm) {
			CompletableFuture.allOf(managedForm.getSubQueries().values().stream().map(managedQuery -> executeAsync((ManagedQuery) managedQuery.resolve(), executionManager)).toArray(CompletableFuture[]::new))
							 .thenRun(() -> managedForm.finish(ExecutionState.DONE, executionManager));
			return;
		}

		throw new IllegalStateException("Unexpected type of execution: %s".formatted(execution.getClass()));
	}

	@Override
	public void cancelQuery(Dataset dataset, ManagedExecution query) {

		CompletableFuture<Void> sqlQueryExecution = runningExecutions.remove(query.getId());

		// already finished/canceled
		if (sqlQueryExecution == null) {
			return;
		}

		if (!sqlQueryExecution.isCancelled()) {
			sqlQueryExecution.cancel(true);
		}

		query.cancel();
	}

	private CompletableFuture<Void> executeAsync(ManagedQuery managedQuery, SqlExecutionManager executionManager) {
		SqlQuery sqlQuery = converter.convert(managedQuery.getQuery());
		return CompletableFuture.supplyAsync(() -> executionService.execute(sqlQuery))
								.thenAccept(result -> {
									addResult(managedQuery, result);
									managedQuery.setLastResultCount(((long) result.getRowCount()));
									managedQuery.finish(ExecutionState.DONE, executionManager);
									runningExecutions.remove(managedQuery.getId());
								});
	}

}
