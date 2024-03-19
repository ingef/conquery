package com.bakdata.conquery.sql.conquery;


import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.InternalExecution;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.sql.SqlContext;
import com.bakdata.conquery.sql.conversion.SqlConverter;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.execution.SqlExecutionResult;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlExecutionManager extends ExecutionManager<SqlExecutionResult> {

	private final SqlExecutionService executionService;
	private final SqlConverter converter;


	public SqlExecutionManager(final SqlContext context, SqlExecutionService sqlExecutionService, MetaStorage storage) {
		super(storage);
		executionService = sqlExecutionService;
		converter = new SqlConverter(context.getSqlDialect(), context.getConfig());
	}

	@Override
	protected void doExecute(Namespace namespace, InternalExecution<?> execution) {

		if (execution instanceof ManagedQuery managedQuery) {
			// todo(tm): Non-blocking execution
			final SqlQuery sqlQuery = converter.convert(managedQuery.getQuery());

			final SqlExecutionResult result = executionService.execute(sqlQuery);

			addResult(managedQuery, result);

			managedQuery.setLastResultCount(((long) result.getRowCount()));

			managedQuery.finish(ExecutionState.DONE);
			return;
		}

		if (execution instanceof ManagedInternalForm<?> managedForm){
			for (ManagedQuery subQuery : managedForm.getSubQueries().values()) {
				doExecute(namespace, subQuery);
			}
			//TODO handle finishing etc here.
		}

	}

	@Override
	public void cancelQuery(Dataset dataset, ManagedExecution query) {
		// unsupported for now
	}

}
