package com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum;

import java.math.BigDecimal;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

public class DecimalSumAggregator extends SingleColumnAggregator<BigDecimal> {


	private BigDecimal sum = BigDecimal.ZERO;

	public DecimalSumAggregator(Column column) {
		super(column);
	}

	@Override
	public DecimalSumAggregator clone() {
		return new DecimalSumAggregator(getColumn());
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		BigDecimal addend = block.getDecimal(event, getColumn());

		sum = sum.add(addend);
	}

	@Override
	public BigDecimal getAggregationResult() {
		return sum;
	}
}
