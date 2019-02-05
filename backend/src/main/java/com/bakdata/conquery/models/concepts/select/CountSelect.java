package com.bakdata.conquery.models.concepts.select;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountDistinctAggregator;
import com.bakdata.conquery.models.query.select.Select;

import lombok.Getter;
import lombok.Setter;

@CPSType(id = "COUNT", base = Select.class)
public class CountSelect extends ColumnSelect {

	@Getter
	@Setter
	private boolean distinct = false;

	@Override
	protected Aggregator<?> createAggregator() {
		if (distinct) {
			return new CountDistinctAggregator(getColumn());
		}
		else {
			return new CountAggregator(getColumn());
		}
	}
}
