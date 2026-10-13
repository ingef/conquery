package com.bakdata.conquery.sql.translate.query.aggregation;


@FunctionalInterface
interface DateAggregationCteConstructor {
	DateAggregationCte create(DateAggregationCteStep cteStep);
};
