package com.bakdata.conquery.models.concepts.select;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.FirstValueAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.date.FirstDateAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.string.FirstStringAggregator;
import com.bakdata.conquery.models.query.select.Select;

@CPSType(id = "FIRST", base = Select.class)
public class FirstValueSelect extends ColumnSelect {

	@Override
	public Aggregator<?> createAggregator() {
		switch (getColumn().getType()) {
			case DATE:
				return new FirstDateAggregator(getColumn());
			case STRING:
				return new FirstStringAggregator(getColumn());
			default:
				return new FirstValueAggregator(getColumn());
		}
	}
}
