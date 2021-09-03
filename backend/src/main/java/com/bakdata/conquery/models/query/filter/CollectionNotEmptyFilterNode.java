package com.bakdata.conquery.models.query.filter;

import java.util.Collection;

import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;


/**
 * Entity is included, when the collection is not empty.
 */
public class CollectionNotEmptyFilterNode<FILTER_VALUE> extends AggregationResultFilterNode<Aggregator<Collection<?>>, FILTER_VALUE> {

	public CollectionNotEmptyFilterNode(Aggregator<Collection<?>> aggregator) {
		super(aggregator, null);
	}

	@Override
	public boolean isContained() {
		return !getAggregator().createAggregationResult().isEmpty();
	}
}
