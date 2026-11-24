package com.bakdata.conquery.models.datasets.concepts.filters;

import com.bakdata.conquery.models.query.queryplan.filter.AggregationFilterNode;

/**
 * Base class for filters that are evaluated on an aggregation result.
 *
 * @param <FILTER_VALUE> type of the filter value
 */
public abstract non-sealed class AggregationFilter<FILTER_VALUE> extends Filter<FILTER_VALUE> {

	@Override
	public abstract AggregationFilterNode<?, ?> createFilterNode(FILTER_VALUE filterValue);
}
