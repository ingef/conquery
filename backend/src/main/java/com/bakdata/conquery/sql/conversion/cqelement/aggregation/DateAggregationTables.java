package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Map;

import com.bakdata.conquery.sql.conversion.model.SqlTables;

class DateAggregationTables<C extends DateAggregationStep> extends SqlTables<C> {

	public DateAggregationTables(Map<C, String> cteNames, String joinedTable) {
		super(joinedTable, cteNames);
	}

}
