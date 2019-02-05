package com.bakdata.conquery.models.concepts.select;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.DistinctValuesAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.date.DistinctDatesAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.string.DistinctStringsAggregator;
import com.bakdata.conquery.models.query.select.Select;

@CPSType(id = "DISTINCT", base = Select.class)
public class DistinctValueSelect extends ColumnSelect {

	@Override
	public Aggregator<?> createAggregator() {
		switch (getColumn().getType()) {
			case DATE:
				return new DistinctDatesAggregator(getColumn());
			case STRING:
				return new DistinctStringsAggregator(getColumn());
			default:
				return new DistinctValuesAggregator(getColumn());
		}
	}
}
