package com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum;

import java.math.BigDecimal;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator implementing a sum over {@code column}, for decimal columns.
 */
public class DecimalSumAggregator extends SingleColumnAggregator<BigDecimal> {

	private BigDecimal sum = BigDecimal.ZERO;

	public DecimalSumAggregator(Column column) {
		super(column);
	}

	@Override
	public DecimalSumAggregator doClone(CloneContext ctx) {
		return new DecimalSumAggregator(getColumn());
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		setHit();

		BigDecimal addend = bucket.getDecimal(event, getColumn());

		sum = sum.add(addend);
	}

	@Override
	public BigDecimal doGetAggregationResult() {
		return sum;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.NUMERIC;
	}
}
