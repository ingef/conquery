package com.bakdata.conquery.models.datasets.concepts.filters;

import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;

public abstract non-sealed class EventFilter<FILTER_VALUE> extends Filter<FILTER_VALUE> {

	@Override
	public abstract EventFilterNode createFilterNode(FILTER_VALUE filterValue);

}
