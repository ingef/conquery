package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.SqlTables;

class DateAggregationTables<C extends DateAggregationCteStep> extends SqlTables<C> {

	public DateAggregationTables(String nodeLabel, Set<C> requiredSteps, String rootTableName, NameGenerator nameGenerator) {
		super(nodeLabel, requiredSteps, rootTableName, nameGenerator);
	}

}
