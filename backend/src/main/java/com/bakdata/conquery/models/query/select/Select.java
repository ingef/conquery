package com.bakdata.conquery.models.query.select;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.specific.AggregatorNode;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public abstract class Select  {

	public AggregatorNode<?> createAggregator(int position) {
		return new AggregatorNode<>(position, createAggregator());
	}

	protected abstract Aggregator<?> createAggregator();
}
