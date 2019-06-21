package com.bakdata.eva.query.selects;


import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.eva.query.aggregators.PeriodSumAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id = "PERIOD_SUM", base = Select.class)
public class PeriodSumSelect extends SingleColumnSelect {

	@JsonCreator
	public PeriodSumSelect(@NsIdRef Column column) {
		super(column);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new PeriodSumAggregator(getColumn());
	}
}
