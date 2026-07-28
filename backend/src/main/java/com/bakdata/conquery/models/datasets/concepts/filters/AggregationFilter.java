package com.bakdata.conquery.models.datasets.concepts.filters;

import com.bakdata.conquery.models.query.queryplan.filter.AggregationResultFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;

public abstract non-sealed class AggregationFilter<FILTER_VALUE> extends Filter<FILTER_VALUE> {

	@Override
	public abstract AggregationResultFilterNode createFilterNode(FILTER_VALUE filterValue);

}
