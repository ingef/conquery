package com.bakdata.conquery.models.query.filter;

import java.util.Collection;

import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;

import lombok.extern.slf4j.Slf4j;

/**
 * Includes entities when the specified column is one of many values.
 */
@Slf4j
public class CollectionNotEmptyFilterNode<FILTER_VALUE> extends AggregationResultFilterNode<Aggregator<Collection<?>>, FILTER_VALUE> {

	public CollectionNotEmptyFilterNode(Aggregator<Collection<?>> aggregator) {
		super(aggregator, null);
	}

	@Override
	public CollectionNotEmptyFilterNode<FILTER_VALUE> clone(QueryPlan plan, QueryPlan clone) {
		return new CollectionNotEmptyFilterNode<>(getAggregator().clone());
	}

	@Override
	public boolean isContained() {
		return !getAggregator().getAggregationResult().isEmpty();
	}
}
