package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.EnumSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;
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
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE, MajorTypeId.DATE_RANGE);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new DateUnionAggregator(getColumn());
	}
}
