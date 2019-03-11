package com.bakdata.conquery.models.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.RandomValueAggregator;

@CPSType(id = "RANDOM", base = Select.class)
public class RandomValueSelect extends SingleColumnSelect {


	@Override
	protected Aggregator<?> createAggregator() {
		return new RandomValueAggregator(getColumn());
	}

	@Override
	public ResultType getResultType() {
		return resolveResultType(getColumn().getType());
	}
}
