package com.bakdata.conquery.models.concepts.select.connector.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DateUnionAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id = "DATE_UNION", base = Select.class)
public class DateUnionSelect extends SingleColumnSelect {

	@JsonCreator
	public DateUnionSelect(@NsIdRef Column column) {
		super(column);
	}

	@Override
	public Aggregator<?> createAggregator() {
		switch (getColumn().getType()) {
			case DATE:
			case DATE_RANGE:
				return new DateUnionAggregator(getColumn());
			default:
				throw new IllegalStateException(String.format("Date Union requires either DateRange or Dates, not %s", getColumn()));
		}
	}
}
