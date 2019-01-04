package com.bakdata.conquery.models.query.aggregators.filter;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.concepts.filters.specific.DurationSumFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

/**
 * Entity is included once a DateRange has a length in the specific range.
 */
public class DurationLengthFilterNode extends FilterNode<FilterValue.CQIntegerRangeFilter, DurationSumFilter> {

	public DurationLengthFilterNode(DurationSumFilter durationSumFilter, FilterValue.CQIntegerRangeFilter filterValue) {
		super(durationSumFilter, filterValue);
	}

	@Override
	public DurationLengthFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new DurationLengthFilterNode(filter, filterValue);
	}

	@Override
	protected OpenResult nextEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return OpenResult.MAYBE;
		}

		CDateRange dateRange = block.getDateRange(event, filter.getColumn());
		long duration = dateRange.getDurationInDays();

		if (filterValue.getValue().contains(duration)) {
			return OpenResult.INCLUDED;
		}

		return OpenResult.MAYBE;
	}
}
