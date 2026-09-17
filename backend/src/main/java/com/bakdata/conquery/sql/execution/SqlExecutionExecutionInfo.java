package com.bakdata.conquery.sql.execution;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.Data;
import lombok.Setter;

@Data
public class SqlExecutionExecutionInfo implements ExecutionManager.InternalExecutionInfo {

	@Setter
	ExecutionState executionState;
	List<String> columnNames;
	List<EntityResult> table;
	private List<ResultInfo> resultInfos;
	CountDownLatch executingLock;

	public SqlExecutionExecutionInfo() {
		this.executionState = ExecutionState.RUNNING;
		this.columnNames = null;
		this.table = null;
		this.executingLock = new CountDownLatch(1);
	}

	public SqlExecutionExecutionInfo(ExecutionState executionState, List<String> columnNames, List<EntityResult> table, List<ResultInfo> resultInfos, CountDownLatch executingLock) {
		this.executionState = executionState;
		this.columnNames = columnNames;
		this.resultInfos = resultInfos;
		this.table = table;
		this.executingLock = executingLock;
	}

	@Override
	public Stream<EntityResult> streamQueryResults() {
		// when the SQL execution fails, table is null
		if (table == null) {
			return Stream.empty();
		}
		return table.stream();
	}

	@Override
	public long getResultCount() {
		if (table == null) {
			return 0;
		}
		return table.size();
	}
}
