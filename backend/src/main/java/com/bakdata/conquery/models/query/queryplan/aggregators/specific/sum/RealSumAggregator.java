package com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

public class RealSumAggregator extends SingleColumnAggregator<Double> {


	private double sum;

	public RealSumAggregator(Column column) {
		super(column);
		this.sum = 0d;
	}

	@Override
	public RealSumAggregator clone() {
		return new RealSumAggregator(getColumn());
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
