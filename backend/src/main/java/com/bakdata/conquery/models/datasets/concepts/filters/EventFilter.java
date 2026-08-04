package com.bakdata.conquery.models.datasets.concepts.filters;

import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;

/**
 * Base class for filters that are evaluated for each event.
 *
 * @param <FILTER_VALUE> type of the filter value
 */
public abstract non-sealed class EventFilter<FILTER_VALUE> extends Filter<FILTER_VALUE> {

	@Override
	public abstract EventFilterNode<?> createFilterNode(FILTER_VALUE filterValue);
}
