package com.bakdata.conquery.sql.conquery;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.sql.conversion.SqlConverter;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.execution.SqlExecutionResult;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import lombok.extern.slf4j.Slf4j;

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
	protected <E extends ManagedExecution & InternalExecution<?>> void doExecute(E execution) {

		addResult(execution.getId(), new SqlExecutionResult());

		if (execution instanceof ManagedQuery managedQuery) {
			CompletableFuture<Void> sqlQueryExecution = executeAsync(managedQuery, this);
			runningExecutions.put(managedQuery.getId(), sqlQueryExecution);
			return;
		}

		if (execution instanceof ManagedInternalForm<?> managedForm) {
			CompletableFuture.allOf(managedForm.getSubQueries().values().stream().map(managedQuery -> {
						addResult(managedQuery, new SqlExecutionResult());
						return executeAsync((ManagedQuery) managedQuery.resolve(), this);

					}).toArray(CompletableFuture[]::new))
					.thenRun(() -> {
						managedForm.finish(ExecutionState.DONE, this);
						clearLock(managedForm.getId());
					});
			return;
		}

		throw new IllegalStateException("Unexpected type of execution: %s".formatted(execution.getClass()));
	}

	@Override
	public void doCancelQuery(ManagedExecution execution) {

		CompletableFuture<Void> sqlQueryExecution = runningExecutions.remove(execution.getId());

		// already finished/canceled
		if (sqlQueryExecution == null) {
			return;
		}

		if (!sqlQueryExecution.isCancelled()) {
			sqlQueryExecution.cancel(true);
		}

		execution.cancel();
	}

	private CompletableFuture<Void> executeAsync(ManagedQuery managedQuery, SqlExecutionManager executionManager) {
		SqlQuery sqlQuery = converter.convert(managedQuery.getQuery());
		return CompletableFuture.supplyAsync(() -> executionService.execute(sqlQuery))
								.thenAccept(result -> {
									ManagedExecutionId id = managedQuery.getId();
									try {
										// We need to transfer the columns and data from the query result together with the execution lock to a new result
										SqlExecutionResult startResult = getResult(id);
										SqlExecutionResult finishResult = new SqlExecutionResult(result.getColumnNames(), result.getTable(), startResult.getExecutingLock());
										addResult(id, finishResult);
									} catch (ExecutionException e) {
										throw new RuntimeException(e);
									}
									managedQuery.setLastResultCount(((long) result.getRowCount()));
									managedQuery.finish(ExecutionState.DONE, executionManager);
									runningExecutions.remove(id);

									// Unlock waiting requests
									clearLock(id);
								})
								.exceptionally(e -> {
									managedQuery.fail(ConqueryError.asConqueryError(e), this);
									runningExecutions.remove(managedQuery.getId());
									return null;
								});
	}

}
