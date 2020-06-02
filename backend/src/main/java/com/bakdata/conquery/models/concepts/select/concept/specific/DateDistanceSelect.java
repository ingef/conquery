package com.bakdata.conquery.models.concepts.select.concept.specific;

import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.date.DateDistanceAggregator;
import lombok.Getter;
import lombok.Setter;

@CPSType(id = "DATE_DISTANCE", base = Select.class)
@Getter @Setter
public class DateDistanceSelect extends UniversalSelect {

	private ChronoUnit timeUnit = ChronoUnit.YEARS;

	@Override
	public Aggregator<?> createAggregator() {
		return new DateDistanceAggregator(getTimeUnit());
	}
}
