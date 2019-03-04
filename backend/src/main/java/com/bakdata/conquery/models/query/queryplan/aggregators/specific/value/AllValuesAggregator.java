package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

public class AllValuesAggregator<VALUE> extends SingleColumnAggregator<Set<VALUE>> {

	private final Set<VALUE> entries = new HashSet<>();

	public AllValuesAggregator(Column column) {
		super(column);
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (block.has(event, getColumn())) {
			entries.add((VALUE) getColumn().getTypeFor(block).createPrintValue(block.getAsObject(event, getColumn())));
		}
	}

	@Override
	public Set<VALUE> getAggregationResult() {
		return entries;
	}

	@Override
	public AllValuesAggregator clone() {
		return new AllValuesAggregator(getColumn());
	}
}
