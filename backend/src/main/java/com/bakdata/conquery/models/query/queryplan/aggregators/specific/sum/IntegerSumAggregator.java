package com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator implementing a sum over {@code column}, for Integer columns.
 */
public class IntegerSumAggregator extends SingleColumnAggregator<Long> {

	private long sum = 0;

	public IntegerSumAggregator(Column column) {
		super(column);
	}

	@Override
	public IntegerSumAggregator doClone(CloneContext ctx) {
		return new IntegerSumAggregator(getColumn());
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		setHit();

		long addend = bucket.getInteger(event, getColumn());

		sum += addend;
	}

	@Override
	public Long doGetAggregationResult() {
		return sum;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
