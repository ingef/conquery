package com.bakdata.conquery.sql.execution;

import java.util.List;

import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import lombok.Value;

@Value
public class SqlExecutionResult {

	List<String> columnNames;
	List<EntityResult> table;
	int rowCount;

	public SqlExecutionResult(List<String> columnNames, List<EntityResult> table) {
		this.columnNames = columnNames;
		this.table = table;
		this.rowCount = table.size();
	}

}
