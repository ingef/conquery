package com.bakdata.conquery.models.concepts.select;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.RandomValueAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.date.RandomDateAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.string.RandomStringAggregator;
import com.bakdata.conquery.models.query.select.Select;

@CPSType(id = "RANDOM", base = Select.class)
public class RandomValueSelect extends ColumnSelect {

	@Override
	public Aggregator<?> createAggregator() {
		switch (getColumn().getType()) {
			case DATE:
				return new RandomDateAggregator(getColumn());
			case STRING:
				return new RandomStringAggregator(getColumn());
			default:
				return new RandomValueAggregator(getColumn());
		}
	}
}
