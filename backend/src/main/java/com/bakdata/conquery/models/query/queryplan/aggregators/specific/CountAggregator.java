package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import lombok.ToString;

/**
 * Aggregator counting the number of present values in a column.
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class CountAggregator extends SingleColumnAggregator<Long> {

	private long count = 0;

	public CountAggregator(Column column) {
		super(column);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		count = 0;
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (bucket.has(event, getColumn())) {
			count++;
		}
	}

	@Override
	public Long createAggregationResult() {
		return count > 0 ? count : null;
	}

	@Override
	public ResultType getResultType() {
		return ResultType.IntegerT.INSTANCE;
	}
}
