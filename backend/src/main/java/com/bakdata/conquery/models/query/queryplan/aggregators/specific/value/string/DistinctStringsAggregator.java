package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.string;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.types.CType;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity is included when the number of values for a specified column are
 * within a given range.
 */
public class DistinctStringsAggregator extends SingleColumnAggregator<List<String>> {

	private final List<String> entries = new ArrayList<>();

	public DistinctStringsAggregator(SelectId id, Column column) {
		super(id, column);
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (block.has(event, getColumn())) {
			entries.add((String) ((CType<Integer, ?>) getColumn().getTypeFor(block)).createScriptValue(block.getString(event, getColumn())));
		}
	}

	@Override
	public List<String> getAggregationResult() {
		return entries;
	}

	@Override
	public DistinctStringsAggregator clone() {
		return new DistinctStringsAggregator(getId(), getColumn());
	}
}
