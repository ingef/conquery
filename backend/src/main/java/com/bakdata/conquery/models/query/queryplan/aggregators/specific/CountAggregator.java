package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

/**
 * Entity is included when number of events with non null values is within a
 * given range.
 */
public class CountAggregator extends SingleColumnAggregator<Long> {

	private long count = 0;

	public CountAggregator(SelectId id, Column column) {
		super(id, column);
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (block.has(event, getColumn())) {
			count++;
		}
	}

	@Override
	public Long getAggregationResult() {
		return count;
	}

	@Override
	public CountAggregator clone() {
		return new CountAggregator(getId(), getColumn());
	}
}
