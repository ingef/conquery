package com.bakdata.conquery.sql.conquery;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.bakdata.conquery.sql.execution.SqlExecutionState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlExecutionManager extends ExecutionManager {

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
	protected <E extends ManagedExecution & InternalExecution> void doExecute(E execution) {

		addState(execution.getId(), new SqlExecutionState());

		if (execution instanceof ManagedQuery managedQuery) {
			CompletableFuture<Void> sqlQueryExecution = executeAsync(managedQuery, this);
			runningExecutions.put(managedQuery.getId(), sqlQueryExecution);
			return;
		}

		if (execution instanceof ManagedInternalForm<?> managedForm) {
			CompletableFuture.allOf(managedForm.getSubQueries().values().stream().map(managedQuery -> {
								 addState(managedQuery.getId(), new SqlExecutionState());
								 return executeAsync(managedQuery, this);

							 }).toArray(CompletableFuture[]::new))
							 .thenRun(() -> managedForm.finish(ExecutionState.DONE, this));
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

									// We need to transfer the columns and data from the query result together with the execution lock to a new result
									SqlExecutionState startResult = getResult(id);
									SqlExecutionState
											finishResult =
											new SqlExecutionState(result.getColumnNames(), result.getTable(), startResult.getExecutingLock());
									addState(id, finishResult);

									managedQuery.setLastResultCount(((long) result.getRowCount()));
									managedQuery.finish(ExecutionState.DONE, executionManager);
									runningExecutions.remove(id);
								})
								.exceptionally(e -> {
									managedQuery.fail(ConqueryError.asConqueryError(e), this);
									runningExecutions.remove(managedQuery.getId());
									return null;
								});
	}

}
