package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(onConstructor_=@JsonCreator)
public class ConstantValueAggregator implements Aggregator<String> {

	private final String value;
	
	@Override
	public ConstantValueAggregator clone() {
		return this;
	}

	@Override
	public String getAggregationResult() {
		return value;
	}

	@Override
	public void aggregateEvent(Block block, int event) {}
}
