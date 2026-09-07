package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import lombok.Value;

@Value
public class SqlFilters {
	ConnectorSqlSelects selects;
	WhereClauses whereClauses;
}
