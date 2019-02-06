package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity is included when the number of values for a specified column are
 * within a given range.
 */
public class CountDistinctAggregator extends SingleColumnAggregator<Long> {

	private final Set<Object> entries = new HashSet<>();

	public CountDistinctAggregator(SelectId id, Column column) {
		super(id, column);
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
		return new CountDistinctAggregator(getId(), getColumn());
	}
}
