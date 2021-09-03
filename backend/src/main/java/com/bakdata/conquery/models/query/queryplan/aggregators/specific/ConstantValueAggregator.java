package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Helper Aggregator, returning a constant value passed in the constructor.
 */
@Getter
@RequiredArgsConstructor
@ToString
public class ConstantValueAggregator extends Aggregator<Object> {

	private final Object value;
	private final ResultType type;

	@Override
	public Object createAggregationResult() {
		return value;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {

	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {}
	
	@Override
	public ResultType getResultType() {
		return type;
	}
}
