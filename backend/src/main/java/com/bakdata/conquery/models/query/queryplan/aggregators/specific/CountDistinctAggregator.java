package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

/**
 * Entity is included when the number of values for a specified column are
 * within a given range.
 */
public class CountDistinctAggregator extends SingleColumnAggregator<Long> {

	private final Set<Object> entries = new HashSet<>();

	public CountDistinctAggregator(Column column) {
		super(column);
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (block.has(event, getColumn())) {
			entries.add(block.getAsObject(event, getColumn()));
		}
	}

	@Override
	public Long getAggregationResult() {
		return Long.valueOf(entries.size());
	}

	@Override
	public CountDistinctAggregator clone() {
		return new CountDistinctAggregator(getColumn());
	}
}
