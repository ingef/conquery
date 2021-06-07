package com.bakdata.conquery.models.concepts.filters;

import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

public interface AggregationFilter<VALUE> {
	public FilterNode<VALUE> createAggregationFilter(VALUE value);
}
