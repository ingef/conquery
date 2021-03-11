package com.bakdata.conquery.models.query;

/**
 * Determines how the dates of hit events are aggregated for a representation as a column the resulting table.
 */
public enum DateAggregationMode {
    /**
     * Don't generate an aggregated event date column at all in the result.
     */
    NONE,
    /**
     * Merge the dates of all hit events.
     */
    MERGE,
    /**
     * Intersect the dates of all hit events.
     */
    INTERSECT,
    /**
     * Merge or intersect the dates depending on certain nodes in the query plan (OR -> MERGE, AND -> INTERSECT,
     * NOT -> INVERT)
     */
    LOGICAL;
}
