package com.bakdata.conquery.models.concepts.select.connector.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DurationSumAggregatorNode;

@CPSType(id = "DURATION_SUM", base = Select.class)
public class DurationSumSelect extends SingleColumnSelect {

	public DurationSumSelect(@NsIdRef Column column) {
		super(column);
	}

	@Override
	protected Aggregator<?> createAggregator() {
		switch (getColumn().getType()) {
			case DATE_RANGE:
				return new DurationSumAggregatorNode(getColumn());
			default:
				throw new IllegalStateException(String.format("Duration Sum requires either DateRange or Dates, not %s", getColumn()));
		}
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
