package com.bakdata.conquery.sql.execution;

import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.Value;

@Value
public class SqlExecutionResult implements ExecutionManager.Result {

	List<String> columnNames;
	List<EntityResult> table;
	int rowCount;

	public SqlExecutionResult(List<String> columnNames, List<EntityResult> table) {
		this.columnNames = columnNames;
		this.table = table;
		rowCount = table.size();
	}

	@Override
	public Stream<EntityResult> streamQueryResults() {
		return table.stream();
	}
}
