package com.bakdata.conquery.models.query.queryplan.aggregators;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.EventIterating;
import com.bakdata.conquery.models.query.queryplan.clone.CtxCloneable;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Aggregator<T> extends CtxCloneable<Aggregator<T>>, EventIterating {

	T getAggregationResult();

	void aggregateEvent(Block block, int event);
	
	@JsonIgnore
	ResultType getResultType();
}
