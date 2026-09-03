package com.bakdata.conquery.models.query;

/** Defines how validity dates produced by child query nodes are propagated. */
public enum DateAggregationAction {
	BLOCK,
	MERGE,
	INTERSECT,
	NEGATE
}
