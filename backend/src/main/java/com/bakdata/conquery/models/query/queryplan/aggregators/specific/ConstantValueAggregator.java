package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Helper Aggregator, returning a constant value passed in the constructor.
 */
@Getter
@RequiredArgsConstructor
@ToString
public class ConstantValueAggregator implements Aggregator<Object> {

	private final Object value;
	private final ResultType type;
	@Getter
	private boolean hit = false;
	
	@Override
	public ConstantValueAggregator doClone(CloneContext ctx) {
		return this;
	}

	@Override
	public Object getAggregationResult() {
		return value;
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		hit = true;
	}
	
	@Override
	public ResultType getResultType() {
		return type;
	}
}
