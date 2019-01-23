package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity is included when the number of values for a specified column are
 * within a given range.
 */
public class ValuesAggregator extends SingleColumnAggregator<Set<Object>> {

	private final Set<Object> entries = new HashSet<>();

	public ValuesAggregator(Column column) {
		super(column);
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (block.has(event, getColumn())) {
			entries.add(block.getAsObject(event, getColumn()));
		}
	}

	@Override
	public Set<Object> getAggregationResult() {
		return entries;
	}

	@Override
	public ValuesAggregator clone() {
		return new ValuesAggregator(getColumn());
	}
}
