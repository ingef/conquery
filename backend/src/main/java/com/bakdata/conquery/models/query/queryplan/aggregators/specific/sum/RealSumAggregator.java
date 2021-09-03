package com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

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

		double addend = bucket.getReal(event, getColumn());

		sum += addend;
	}

	@Override
	public Double createAggregationResult() {
		return hit ? sum : null;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.NumericT.INSTANCE;
	}
}
