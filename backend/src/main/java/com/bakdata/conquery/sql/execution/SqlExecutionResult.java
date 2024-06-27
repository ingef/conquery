package com.bakdata.conquery.sql.execution;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.Value;

@Value
public class SqlExecutionResult implements ExecutionManager.InternalResult {

	List<String> columnNames;
	List<EntityResult> table;
	int rowCount;
	CountDownLatch executingLock;

	public SqlExecutionResult() {
		this.columnNames = null;
		this.table = null;
		this.executingLock = new CountDownLatch(1);
		rowCount = 0;
	}

	public SqlExecutionResult(List<String> columnNames, List<EntityResult> table, CountDownLatch executingLock) {
		this.columnNames = columnNames;
		this.table = table;
		this.executingLock = executingLock;
		rowCount = table.size();
	}

	@Override
	public Stream<EntityResult> streamQueryResults() {
		return table.stream();
	}
}
