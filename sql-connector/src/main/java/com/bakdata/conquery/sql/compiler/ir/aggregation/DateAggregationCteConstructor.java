package com.bakdata.conquery.sql.compiler.ir.aggregation;

@FunctionalInterface
interface DateAggregationCteConstructor {
	DateAggregationCte create(DateAggregationCteStep cteStep);
};
