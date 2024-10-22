package com.bakdata.conquery.sql.conquery;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.sql.conversion.SqlConverter;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.bakdata.conquery.sql.execution.SqlExecutionState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlExecutionManager extends ExecutionManager {

	private final SqlExecutionService executionService;
	private final SqlConverter converter;
	private final ConcurrentMap<ManagedExecutionId, CompletableFuture<Void>> runningExecutions;

	public SqlExecutionManager(SqlConverter sqlConverter, SqlExecutionService sqlExecutionService, MetaStorage storage, DatasetRegistry<?> datasetRegistry, ConqueryConfig config) {
		super(storage, datasetRegistry, config);
		this.converter = sqlConverter;
		this.executionService = sqlExecutionService;
		this.runningExecutions = new ConcurrentHashMap<>();
	}

	@Override
	protected <E extends ManagedExecution & InternalExecution> void doExecute(E execution) {

		addState(execution.getId(), new SqlExecutionState());

		if (execution instanceof ManagedQuery managedQuery) {
			CompletableFuture<Void> sqlQueryExecution = executeAsync(managedQuery);
			runningExecutions.put(managedQuery.getId(), sqlQueryExecution);
			return;
		}

		if (execution instanceof ManagedInternalForm<?> managedForm) {
			CompletableFuture.allOf(managedForm.getSubQueries().values().stream().map(executionId -> {
												   addState(executionId, new SqlExecutionState());
												   return executeAsync((ManagedQuery) executionId.resolve());

											   })
											   .toArray(CompletableFuture[]::new))
							 .thenRun(() -> managedForm.finish(ExecutionState.DONE));
			return;
		}

		throw new IllegalStateException("Unexpected type of execution: %s".formatted(execution.getClass()));
	}

	private CompletableFuture<Void> executeAsync(ManagedQuery managedQuery) {
		SqlQuery sqlQuery = converter.convert(managedQuery.getQuery(), managedQuery.getNamespace());

		return CompletableFuture.supplyAsync(() -> executionService.execute(sqlQuery))
								.thenAccept(result -> {
									ManagedExecutionId id = managedQuery.getId();

									// We need to transfer the columns and data from the query result together with the execution lock to a new result
									SqlExecutionState startResult = getResult(id);
									SqlExecutionState
											finishResult =
											new SqlExecutionState(ExecutionState.DONE, result.getColumnNames(), result.getTable(), startResult.getExecutingLock());
									addState(id, finishResult);

									managedQuery.finish(ExecutionState.DONE);
									runningExecutions.remove(id);
								})
								.exceptionally(e -> {
									managedQuery.fail(ConqueryError.asConqueryError(e));
									runningExecutions.remove(managedQuery.getId());
									return null;
								});
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

}
