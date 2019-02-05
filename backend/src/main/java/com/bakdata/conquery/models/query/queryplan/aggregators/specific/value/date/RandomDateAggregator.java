package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.date;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

import java.time.LocalDate;
import java.util.Random;

public class RandomDateAggregator extends SingleColumnAggregator<LocalDate> {

	private int value;
	private int nValues = 0;
	private final Random random = new Random();

	public RandomDateAggregator(Column column) {
		super(column);
	}

	/**
	 * length of sequence = m, but not known at event of sampling
	 * <p>
	 * P(switching n-th value) = 1/n
	 * <p>
	 * P(n-th value = output) 	= P(switching n-th value) * P(not switching values > n)
	 * = 1/n * n/m = 1/m
	 *
	 * @param block
	 * @param event
	 */
	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		// Count how many values we have seen, so a draw is always evenly distributed
		nValues++;

		if (random.nextInt(nValues) == 0) {
			value = block.getDate(event, getColumn());
		}
	}

	@Override
	public LocalDate getAggregationResult() {
		return CDate.toLocalDate(value);
	}

	@Override
	public RandomDateAggregator clone() {
		return new RandomDateAggregator(getColumn());
	}
}
