package com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

public class RealSumAggregator extends SingleColumnAggregator<Double> {


	private double sum = 0d;

	public RealSumAggregator(SelectId id, Column column) {
		super(id, column);
	}

	@Override
	public RealSumAggregator clone() {
		return new RealSumAggregator(getId(), getColumn());
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		double addend = block.getReal(event, getColumn());

		sum += addend;
	}

	@Override
	public Double getAggregationResult() {
		return sum;
	}
}
