package com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.SimpleResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator implementing a sum over {@code column}, for real columns.
 */
public class RealSumAggregator extends SingleColumnAggregator<Double> {

	private boolean hit = false;
	private double sum = 0d;

	public RealSumAggregator(Column column) {
		super(column);
	}

	@Override
	public RealSumAggregator doClone(CloneContext ctx) {
		return new RealSumAggregator(getColumn());
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		hit = true;

		double addend = bucket.getReal(event, getColumn());

		sum += addend;
	}

	@Override
	public Double getAggregationResult() {
		return hit ? sum : null;
	}
	
	@Override
	public SimpleResultType getResultType() {
		return SimpleResultType.NUMERIC;
	}
}
