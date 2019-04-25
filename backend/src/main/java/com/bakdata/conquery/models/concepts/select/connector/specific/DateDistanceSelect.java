package com.bakdata.conquery.models.concepts.select.connector.specific;

import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DateDistanceAggregatorNode;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

@CPSType(id = "DATE_DISTANCE", base = Select.class)
@Getter @Setter
public class DateDistanceSelect extends SingleColumnSelect {

	@JsonCreator
	public DateDistanceSelect() {
		super(null);
	}

	private ChronoUnit timeUnit = ChronoUnit.YEARS;


	@Override
	public Aggregator<?> createAggregator() {
		return new DateDistanceAggregatorNode(getColumn(), getTimeUnit());
	}
}
