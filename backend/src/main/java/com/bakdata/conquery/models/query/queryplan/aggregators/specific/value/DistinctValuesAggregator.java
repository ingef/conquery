package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity is included when the number of values for a specified column are
 * within a given range.
 */
public class DistinctValuesAggregator extends SingleColumnAggregator<List<Object>> {

	private final List<Object> entries = new ArrayList<>();

	public DistinctValuesAggregator(SelectId id, Column column) {
		super(id, column);
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (block.has(event, getColumn())) {
			entries.add(block.getAsObject(event, getColumn()));
		}
	}

	@Override
	public List<Object> getAggregationResult() {
		return entries;
	}

	@Override
	public DistinctValuesAggregator clone() {
		return new DistinctValuesAggregator(getId(), getColumn());
	}
}
