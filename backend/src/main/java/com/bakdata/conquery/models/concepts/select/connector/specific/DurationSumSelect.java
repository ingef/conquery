package com.bakdata.conquery.models.concepts.select.connector.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DurationSumAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id = "DURATION_SUM", base = Select.class)
public class DurationSumSelect extends SingleColumnSelect {

	@JsonCreator
	public DurationSumSelect(@NsIdRef Column column) {
		super(column);
	}

	@Override
	public Aggregator<?> createAggregator() {
		switch (getColumn().getType()) {
			case DATE:
			case DATE_RANGE:
				return new DurationSumAggregator(getColumn());
			default:
				throw new IllegalStateException(String.format("Duration Sum requires either DateRange or Dates, not %s", getColumn()));
		}
	}
}
