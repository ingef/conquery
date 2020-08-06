package com.bakdata.conquery.models.concepts.select.concept;

import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.UniversalAggregator;

public abstract class UniversalSelect extends Select {
	@Override
	public abstract UniversalAggregator<?> createAggregator();
}
