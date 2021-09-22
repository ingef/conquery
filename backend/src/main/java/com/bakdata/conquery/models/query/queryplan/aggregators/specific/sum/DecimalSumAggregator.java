package com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum;

import java.math.BigDecimal;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

/**
 * Aggregator implementing a sum over {@code column}, for decimal columns.
 */
public class DecimalSumAggregator extends SingleColumnAggregator<BigDecimal> {

	private boolean hit = false;
	private BigDecimal sum = BigDecimal.ZERO;

	public DecimalSumAggregator(Column column) {
		super(column);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		hit = false;
		sum = BigDecimal.ZERO;
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		hit = true;

		BigDecimal addend = bucket.getDecimal(event, getColumn());

		sum = sum.add(addend);
	}

	@Override
	public BigDecimal createAggregationResult() {
		return hit ? sum : null;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.NumericT.INSTANCE;
	}
}
