package com.bakdata.conquery.models.query.aggregators.filter;

import com.bakdata.conquery.models.concepts.filters.specific.DateDistanceFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

/**
 * Entity is included as long as Dates are within a certain range.
 */
public class DateDistanceFilterNode extends FilterNode<FilterValue.CQIntegerRangeFilter, DateDistanceFilter> {
	private int min;
	private int max;

	public DateDistanceFilterNode(DateDistanceFilter dateDistanceFilter, FilterValue.CQIntegerRangeFilter filterValue) {
		super(dateDistanceFilter, filterValue);
	}

	@Override
	public FilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new DateDistanceFilterNode(filter, filterValue);
	}

	@Override
	public OpenResult nextEvent(Block block, int event) {
		if (block.has(event, filter.getColumn())) {

			//TODO rewrite according to SQL

			int value = block.getDate(event, filter.getColumn());

			if (value < min) {
				min = value;
			}

			if (value > max) {
				max = value;
			}
		}

		return OpenResult.MAYBE;
	}

	@Override
	public boolean isContained() {
		return filterValue.getValue().contains(max - min);
	}
}
