package com.bakdata.conquery.models.query.filter;

import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Entity is included as when one column is equal to the selected value.
 */
@Slf4j
public class ValueAboveZeroFilterNode<FILTER_VALUE, FILTER extends Filter<FILTER_VALUE>> extends AggregationResultFilterNode<Aggregator<Long>, FILTER_VALUE, FILTER> {


	public ValueAboveZeroFilterNode(FILTER filter, Aggregator<Long> aggregator) {
		super(aggregator, filter, null);
	}

	@Override
	public FilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new ValueAboveZeroFilterNode(filter, getAggregator().clone());
	}

	@Override
	public boolean isContained() {
		return getAggregator().getAggregationResult() > 0;
	}
}
