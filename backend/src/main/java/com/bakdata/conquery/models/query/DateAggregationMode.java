package com.bakdata.conquery.models.query;

import com.bakdata.conquery.apiv1.query.concept.specific.CQOr;

/**
 * Determines how the dates of hit events are aggregated for a representation as a column the resulting table.
 * This mode causes a specific {@link com.bakdata.conquery.models.query.queryplan.DateAggregationAction} to be set
 * in certain query nodes that influence the date aggregation (see {@link CQOr} and {@link com.bakdata.conquery.models.query.queryplan.specific.OrNode} and others).
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
