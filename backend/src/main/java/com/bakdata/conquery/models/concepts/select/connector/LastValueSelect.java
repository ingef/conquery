package com.bakdata.conquery.models.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.LastValueAggregator;

@CPSType(id = "LAST", base = Select.class)
public class LastValueSelect extends SingleColumnSelect {

	@Override
	public Aggregator<?> createAggregator() {
		return new LastValueAggregator(getColumn());
	}
}
