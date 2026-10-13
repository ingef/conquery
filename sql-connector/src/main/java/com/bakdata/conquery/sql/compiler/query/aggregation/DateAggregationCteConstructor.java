package com.bakdata.conquery.sql.compiler.query.aggregation;


@FunctionalInterface
interface DateAggregationCteConstructor {
	DateAggregationCte create(DateAggregationCteStep cteStep);
};
