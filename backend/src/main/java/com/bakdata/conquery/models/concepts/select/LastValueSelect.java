package com.bakdata.conquery.models.concepts.select;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.LastValueAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.date.LastDateAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.string.LastStringAggregator;
import com.bakdata.conquery.models.query.select.Select;

@CPSType(id = "LAST", base = Select.class)
public class LastValueSelect extends ColumnSelect {


	@Override
	protected Aggregator<?> createAggregator() {
		switch (getColumn().getType()) {
			case DATE:
				return new LastDateAggregator(getColumn());
			case STRING:
				return new LastStringAggregator(getColumn());
			default:
				return new LastValueAggregator(getColumn());
		}
	}
}
