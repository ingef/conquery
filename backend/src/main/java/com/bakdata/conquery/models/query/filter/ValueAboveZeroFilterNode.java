package com.bakdata.conquery.models.query.filter;

import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Entity is included as when one column is equal to the selected value.
 */
@Slf4j
public class ValueAboveZeroFilterNode<FILTER_VALUE> extends AggregationResultFilterNode<Aggregator<Long>, FILTER_VALUE> {


	public ValueAboveZeroFilterNode(Aggregator<Long> aggregator) {
		super(aggregator, null);
	}

	@Override
	public FilterNode doClone(CloneContext ctx) {
		return new ValueAboveZeroFilterNode(getAggregator().doClone(ctx));
	}

	@Override
	public boolean isContained() {
		return getAggregator().getAggregationResult() > 0;
	}
}
