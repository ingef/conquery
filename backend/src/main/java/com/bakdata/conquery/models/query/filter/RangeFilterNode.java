package com.bakdata.conquery.models.query.filter;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Entity is included, when the result of the aggregator is contained in the range.
 */
public class RangeFilterNode<TYPE extends Comparable<?>> extends AggregationResultFilterNode<Aggregator<TYPE>, IRange<TYPE, ?>> {


	public RangeFilterNode(IRange<TYPE, ?> filterValue, Aggregator<TYPE> aggregator) {
		super(aggregator, filterValue);
	}

	@Override
	public RangeFilterNode<TYPE> doClone(CloneContext context) {
		return new RangeFilterNode<>(filterValue, getAggregator().doClone(context));
	}

	@Override
	public boolean isContained() {
		return filterValue.contains(getAggregator().getAggregationResult());
	}
}
