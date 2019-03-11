package com.bakdata.conquery.models.query.filter;

import java.util.Collection;

import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;

import lombok.extern.slf4j.Slf4j;

/**
 * Includes entities when the specified column is one of many values.
 */
@Slf4j
public class CollectionNotEmptyFilterNode<FILTER_VALUE> extends AggregationResultFilterNode<Aggregator<Collection<?>>, FILTER_VALUE, Filter<FILTER_VALUE>> {

	public CollectionNotEmptyFilterNode(Filter<FILTER_VALUE> multiSelectFilter, Aggregator<Collection<?>> aggregator) {
		super(aggregator, multiSelectFilter, null);
	}

	@Override
	public CollectionNotEmptyFilterNode<FILTER_VALUE> clone(QueryPlan plan, QueryPlan clone) {
		return new CollectionNotEmptyFilterNode<>(filter, getAggregator().clone());
	}

	@Override
	public boolean isContained() {
		return !getAggregator().getAggregationResult().isEmpty();
	}
}
