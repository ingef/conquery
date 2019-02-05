package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.string;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.types.CType;

import java.util.Random;

public class RandomStringAggregator extends SingleColumnAggregator<String> {

	private String value;
	private int nValues = 0;
	private final Random random = new Random();

	public RandomStringAggregator(Column column) {
		super(column);
	}

	/**
	 * length of sequence = m, but not known at time of sampling
	 * <p>
	 * P(switching n-th value) = 1/n
	 *
	 * P(n-th value = output) 	= P(switching n-th value) * P(not switching values > n)
	 * 	= 1/n * n/m = 1/m
	 * </p>
	 */
	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		// Count how many values we have seen, so a draw is always evenly distributed
		nValues++;

		if (random.nextInt(nValues) == 0) {
			value = (String) ((CType<Integer, ?>) getColumn().getTypeFor(block)).createScriptValue(block.getString(event, getColumn()));
		}
	}

	@Override
	public String getAggregationResult() {
		return value;
	}

	@Override
	public RandomStringAggregator clone() {
		return new RandomStringAggregator(getColumn());
	}
}
