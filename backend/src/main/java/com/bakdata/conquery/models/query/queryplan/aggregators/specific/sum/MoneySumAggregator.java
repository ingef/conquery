package com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator implementing a sum over {@code column}, for money columns.
 */
public class MoneySumAggregator extends SingleColumnAggregator<Long> {

	private long sum = 0L;

	public MoneySumAggregator(Column column) {
		super(column);
	}

	@Override
	public MoneySumAggregator doClone(CloneContext ctx) {
		return new MoneySumAggregator(getColumn());
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		setHit();

		long addend = bucket.getMoney(event, getColumn());

		sum = sum + addend;
	}

	@Override
	public Long doGetAggregationResult() {
		return sum;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.MONEY;
	}
}
