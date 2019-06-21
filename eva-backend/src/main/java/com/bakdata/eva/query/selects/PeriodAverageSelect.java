package com.bakdata.eva.query.selects;


import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.eva.query.aggregators.PeriodAverageAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id = "PERIOD_AVERAGE", base = Select.class)
public class PeriodAverageSelect extends SingleColumnSelect {

	@JsonCreator
	public PeriodAverageSelect(@NsIdRef Column column) {
		super(column);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new PeriodAverageAggregator(getColumn());
	}
}
