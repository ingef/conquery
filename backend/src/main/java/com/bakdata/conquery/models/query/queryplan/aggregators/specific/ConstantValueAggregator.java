package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ConstantValueAggregator implements Aggregator<String> {

	private final String value;
	
	@Override
	public ConstantValueAggregator doClone(CloneContext ctx) {
		return this;
	}

	@Override
	public String getAggregationResult() {
		return value;
	}

	@Override
	public void aggregateEvent(Block block, int event) {}
}
