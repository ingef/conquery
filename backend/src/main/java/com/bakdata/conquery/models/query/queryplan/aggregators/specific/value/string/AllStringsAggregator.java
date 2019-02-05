package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.string;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.types.CType;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity is included when the number of values for a specified column are
 * within a given range.
 */
public class AllStringsAggregator extends SingleColumnAggregator<Set<String>> {

	private final Set<String> entries = new HashSet<>();

	public AllStringsAggregator(Column column) {
		super(column);
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (block.has(event, getColumn())) {
			entries.add((String) ((CType<Integer, ?>) getColumn().getTypeFor(block)).createScriptValue(block.getString(event, getColumn())));
		}
	}

	@Override
	public Set<String> getAggregationResult() {
		return entries;
	}

	@Override
	public AllStringsAggregator clone() {
		return new AllStringsAggregator(getColumn());
	}
}
