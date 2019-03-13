package com.bakdata.conquery.models.query.filter;

import java.util.Collection;

import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Includes entities when the specified column is one of many values.
 */
@Slf4j
public class CollectionNotEmptyFilterNode<FILTER_VALUE extends FilterValue<?>> extends AggregationResultFilterNode<Aggregator<Collection<?>>, FILTER_VALUE, Filter<FILTER_VALUE>> {

	public CollectionNotEmptyFilterNode(Filter<FILTER_VALUE> multiSelectFilter, FILTER_VALUE filterValue, Aggregator<Collection<?>> aggregator) {
		super(aggregator, multiSelectFilter, filterValue);
	}

	@Override
	public CollectionNotEmptyFilterNode<FILTER_VALUE> doClone(CloneContext ctx) {
		return new CollectionNotEmptyFilterNode<>(filter, filterValue, getAggregator().clone(ctx));
	}

	@Override
	public boolean isContained() {
		return !getAggregator().getAggregationResult().isEmpty();
	}
}
