package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;

public interface UniversalAggregator<T> extends Aggregator<T> {
	@Override
	default boolean isAlwaysActive(){
		return true;
	}
}
