package com.bakdata.conquery.models.concepts.select.connector.specific;

import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DateDistanceAggregatorNode;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

@CPSType(id = "DATE_DISTANCE", base = Select.class)
@Getter @Setter
public class DateDistanceSelect extends SingleColumnSelect {

	private ChronoUnit timeUnit = ChronoUnit.YEARS;

	@JsonCreator
	public DateDistanceSelect(Column column) {
		this(column, ChronoUnit.YEARS);
	}

	@JsonCreator
	public DateDistanceSelect(@NsIdRef Column column, ChronoUnit timeUnit) {
		super(column);
		this.timeUnit = timeUnit;
	}

	private ChronoUnit timeUnit = ChronoUnit.YEARS;



	@Override
	public Aggregator<?> createAggregator() {
		return new DateDistanceAggregatorNode(getColumn(), getTimeUnit());
	}
}
