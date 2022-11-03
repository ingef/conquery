package com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.ToString;

/**
 * Aggregator implementing a sum over {@code column}, for Integer columns.
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class IntegerSumAggregator extends SingleColumnAggregator<Long> {

	private boolean hit = false;
	private long sum = 0;

	public IntegerSumAggregator(Column column) {
		super(column);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		hit = false;
		sum = 0;
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		hit = true;

		long addend = bucket.getInteger(event, getColumn());

		sum += addend;
	}

	@Override
	public Long createAggregationResult() {
		return hit ? sum : null;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.IntegerT.INSTANCE;
	}
}
