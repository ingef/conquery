package com.bakdata.conquery.models.concepts.select.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.ColumnSelect;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DurationSumAggregatorNode;
import com.bakdata.conquery.models.query.select.Select;

@CPSType(id = "DURATION_SUM", base = Select.class)
public class DurationSumSelect extends SingleColumnSelect {

	@Override
	protected Aggregator<?> createAggregator() {
		switch (getColumn().getType()) {
			case DATE_RANGE:
				return new DurationSumAggregatorNode(getColumn());
			default:
				throw new IllegalStateException(String.format("Duration Sum requires either DateRange or Dates, not %s", getColumn()));
		}
	}
}
