package com.bakdata.conquery.models.concepts.select.connector.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DurationSumAggregator;

@CPSType(id = "DURATION_SUM", base = Select.class)
public class DurationSumSelect extends Select {

	@Override
	public Aggregator<?> createAggregator() {
		return new DurationSumAggregator();
	}
}
