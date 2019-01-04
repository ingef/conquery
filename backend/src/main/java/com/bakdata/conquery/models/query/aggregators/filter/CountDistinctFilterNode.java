package com.bakdata.conquery.models.query.aggregators.filter;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.concepts.filters.specific.CountFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

/**
 * Entity is included when the number of values for a specified column are within a given range.
 */
public class CountDistinctFilterNode extends CountingAbstractFilterNode<CountFilter> {

	private final Set<Object> entries = new HashSet<>();

	public CountDistinctFilterNode(CountFilter filter, FilterValue.CQIntegerRangeFilter filterValue) {
		super(filter, filterValue);
	}

	@Override
	public long update(Block block, int event) {
		if (block.has(event, filter.getColumn())) {
			entries.add(block.getAsObject(event, filter.getColumn()));
		}

		return entries.size();
	}

	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new CountDistinctFilterNode(filter, filterValue);
	}
}
