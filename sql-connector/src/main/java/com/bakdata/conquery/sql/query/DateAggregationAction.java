package com.bakdata.conquery.sql.query;

/** Defines how validity dates produced by child nodes are propagated. */
public enum DateAggregationAction {
	BLOCK,
	MERGE,
	INTERSECT,
	NEGATE
}
