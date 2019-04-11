package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Entity is included when number of events with non null values is within a
 * given range.
 */
public class CountAggregator extends SingleColumnAggregator<Long> {

	private long count = 0;

	public CountAggregator(Column column) {
		super(column);
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (block.has(event, getColumn())) {
			count++;
		}
	}

	@Override
	public Long getAggregationResult() {
		return count;
	}

	@Override
	public CountAggregator doClone(CloneContext ctx) {
		return new CountAggregator(getColumn());
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.NUMERIC;
	}
}
