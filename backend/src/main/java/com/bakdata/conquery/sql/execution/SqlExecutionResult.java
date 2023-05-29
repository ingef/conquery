package com.bakdata.conquery.sql.execution;

import lombok.Value;

import java.util.List;

@Value
public class SqlExecutionResult {

    List<String> columnNames;
    List<List<String>> table;
    int rowCount;

    public SqlExecutionResult(List<String> columnNames, List<List<String>> table) {
        this.columnNames = columnNames;
        this.table = table;
        this.rowCount = table.size();
    }

}
