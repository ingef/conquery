package com.bakdata.conquery.models.query.aggregators.filter;

import com.bakdata.conquery.models.concepts.filters.specific.CountFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

/**
 * Entity is included when number of events with non null values is within a given range.
 */
public class CountFilterNode extends CountingAbstractFilterNode<CountFilter> {

	private long count = 0;

	public CountFilterNode(CountFilter filter, FilterValue.CQIntegerRangeFilter filterValue) {
		super(filter, filterValue);
	}

	@Override
	protected long update(Block block, int event) {
		if (block.has(event, filter.getColumn())) {
			count++;
		}

		return count;
	}

	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new CountFilterNode(filter, filterValue);
	}
}
