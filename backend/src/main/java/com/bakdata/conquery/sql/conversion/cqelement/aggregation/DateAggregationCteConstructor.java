package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

@FunctionalInterface
interface DateAggregationCteConstructor {
	DateAggregationCte create(DateAggregationCteStep cteStep);
};
