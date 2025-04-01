package com.bakdata.conquery.sql.execution;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.Data;
import lombok.Setter;

@Data
public class SqlExecutionExecutionInfo implements ExecutionManager.InternalExecutionInfo {

	@Setter
	ExecutionState state;
	List<String> columnNames;
	List<EntityResult> table;
	int rowCount;
	CountDownLatch executingLock;

	public SqlExecutionExecutionInfo() {
		this.state = ExecutionState.RUNNING;
		this.columnNames = null;
		this.table = null;
		this.executingLock = new CountDownLatch(1);
		rowCount = 0;
	}

	public SqlExecutionExecutionInfo(ExecutionState state, List<String> columnNames, List<EntityResult> table, CountDownLatch executingLock) {
		this.state = state;
		this.columnNames = columnNames;
		this.table = table;
		this.executingLock = executingLock;
		rowCount = table.size();
	}

	@Override
	public Stream<EntityResult> streamQueryResults() {
		// when the SQL execution fails, table is null
		if (table == null) {
			return Stream.empty();
		}
		return table.stream();
	}
}
