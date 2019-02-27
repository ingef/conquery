package com.bakdata.conquery.models.concepts.select;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.FirstValueAggregator;
import com.bakdata.conquery.models.query.select.Select;

@CPSType(id = "FIRST", base = Select.class)
public class FirstValueSelect extends SingleColumnSelect {

	@Override
	protected Aggregator<?> createAggregator() {
		return new FirstValueAggregator(getColumn());
	}
}
