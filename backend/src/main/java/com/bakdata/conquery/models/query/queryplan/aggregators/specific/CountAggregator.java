package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator counting the number of present values in a column.
 */
public class CountAggregator extends SingleColumnAggregator<Long> {

	private long count = 0;

	public CountAggregator(Column column) {
		super(column);
		unhitDefault = 0L;
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (bucket.has(event, getColumn())) {
			count++;
			setHit();
		}
	}

	@Override
	public Long doGetAggregationResult() {
		return count;
	}

	@Override
	public CountAggregator doClone(CloneContext ctx) {
		return new CountAggregator(getColumn());
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
