package com.bakdata.conquery.models.query.queryplan.aggregators;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.EventIterating;
import com.bakdata.conquery.models.query.queryplan.clone.CtxCloneable;

public interface Aggregator<T> extends CtxCloneable<Aggregator<T>>, EventIterating {

	T getAggregationResult();

	void aggregateEvent(Block block, int event);
}
