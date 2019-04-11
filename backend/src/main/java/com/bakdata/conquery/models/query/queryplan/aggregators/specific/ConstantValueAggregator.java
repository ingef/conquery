package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ConstantValueAggregator implements Aggregator<Object> {

	private final Object value;
	private final ResultType type;
	
	@Override
	public ConstantValueAggregator doClone(CloneContext ctx) {
		return this;
	}

	@Override
	public Object getAggregationResult() {
		return value;
	}

	@Override
	public void aggregateEvent(Block block, int event) {}
	
	@Override
	public ResultType getResultType() {
		return type;
	}
}
