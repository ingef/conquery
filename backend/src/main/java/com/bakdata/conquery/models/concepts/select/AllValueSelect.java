package com.bakdata.conquery.models.concepts.select;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.ValuesAggregator;
import com.bakdata.conquery.models.query.select.Select;

@CPSType(id = "ALL", base = Select.class)
public class AllValueSelect extends ColumnSelect {

	@Override
	protected Aggregator<?> createAggregator() {
		return new ValuesAggregator(getColumn());
	}
}
