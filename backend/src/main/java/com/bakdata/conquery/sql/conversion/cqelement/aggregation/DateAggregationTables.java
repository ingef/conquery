package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.SqlTables;

class DateAggregationTables<C extends DateAggregationStep> extends SqlTables<C> {

	public DateAggregationTables(String nodeLabel, Set<C> requiredSteps, String rootTableName) {
		super(nodeLabel, requiredSteps, rootTableName);
	}

}
