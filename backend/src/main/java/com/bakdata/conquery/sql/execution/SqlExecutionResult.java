package com.bakdata.conquery.sql.execution;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import lombok.Value;

import java.util.List;
import java.util.stream.Stream;

@Value
public class SqlExecutionResult  {

    List<String> columnNames;
    List<SinglelineEntityResult> table;
    int rowCount;

    public SqlExecutionResult(List<String> columnNames, List<SinglelineEntityResult> table) {
		this.columnNames = columnNames;
        this.table = table;
        this.rowCount = table.size();
    }

}
