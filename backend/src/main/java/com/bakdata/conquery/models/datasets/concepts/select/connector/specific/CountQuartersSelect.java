package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.EnumSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountQuartersOfDateRangeAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountQuartersOfDatesAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Entity is included when the number of distinct quarters for all events is within a given range.
 * Implementation is specific for DateRanges
 */
@CPSType(id = "COUNT_QUARTERS", base = Select.class)
public class CountQuartersSelect extends SingleColumnSelect {

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE, MajorTypeId.DATE_RANGE);
	}

	@JsonCreator
	public CountQuartersSelect(@NsIdRef Column column) {
		super(column);
	}

	@Override
	public Aggregator<?> createAggregator() {
		switch (getColumn().getType()) {
			case DATE_RANGE:
				return new CountQuartersOfDateRangeAggregator(getColumn());
			case DATE:
				return new CountQuartersOfDatesAggregator(getColumn());
			default:
				throw new IllegalArgumentException(String.format("Column '%s' is not of Date (-Range) Type but '%s'", getColumn(), getColumn().getType()));
		}

	}
}
