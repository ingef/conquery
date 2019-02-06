package com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

public class IntegerSumAggregator extends SingleColumnAggregator<Long> {


	private long sum = 0;

	public IntegerSumAggregator(SelectId id, Column column) {
		super(id, column);
	}

	@Override
	public IntegerSumAggregator clone() {
		return new IntegerSumAggregator(getId(), getColumn());
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		long addend = block.getInteger(event, getColumn());

		sum += addend;
	}

	@Override
	public Long getAggregationResult() {
		return sum;
	}
}
