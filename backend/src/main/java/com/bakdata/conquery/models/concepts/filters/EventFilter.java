package com.bakdata.conquery.models.concepts.filters;

import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;

public interface EventFilter<VALUE> {
	public EventFilterNode createEventFilter(VALUE value);
}
