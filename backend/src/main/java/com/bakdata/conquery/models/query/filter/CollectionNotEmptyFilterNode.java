package com.bakdata.conquery.models.query.filter;

import java.util.Collection;

import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;


/**
 * Entity is included, when the collection is not empty.
 */
public class CollectionNotEmptyFilterNode<FILTER_VALUE> extends AggregationResultFilterNode<Aggregator<Collection<?>>, FILTER_VALUE> {

	public CollectionNotEmptyFilterNode(Aggregator<Collection<?>> aggregator) {
		super(aggregator, null);
	}

	@Override
	public CollectionNotEmptyFilterNode<FILTER_VALUE> doClone(CloneContext context) {
		return new CollectionNotEmptyFilterNode<>(getAggregator().doClone(context));
	}

	@Override
	public boolean isContained() {
		return !getAggregator().getAggregationResult().isEmpty();
	}
}
