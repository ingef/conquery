package com.bakdata.conquery.models.concepts.select;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.RandomValueAggregator;
import com.bakdata.conquery.models.query.select.Select;

@CPSType(id = "RANDOM", base = Select.class)
public class LastValueSelect extends ColumnSelect {

	@Override
	protected Aggregator<?> createAggregator() {
		return new RandomValueAggregator(getColumn());
	}
}
