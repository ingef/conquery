package com.bakdata.conquery.models.concepts.select.connector.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.QuartersInYearAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Entity is included when the the number of quarters with events is within a specified range.
 */
@CPSType(id = "QUARTERS_IN_YEAR", base = Select.class)
public class QuartersInYearSelect extends SingleColumnSelect {

	@JsonCreator
	public QuartersInYearSelect(@NsIdRef Column column) {
		super(column);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new QuartersInYearAggregator(getColumn());
	}
}
