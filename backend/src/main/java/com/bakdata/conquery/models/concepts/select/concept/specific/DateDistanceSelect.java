package com.bakdata.conquery.models.concepts.select.concept.specific;

import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.date.DateDistanceAggregator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@CPSType(id = "DATE_DISTANCE", base = Select.class)
@Getter
@Setter
@NoArgsConstructor
public class DateDistanceSelect extends SingleColumnSelect {

	private final ChronoUnit timeUnit = ChronoUnit.YEARS;

	public DateDistanceSelect(@NsIdRef @NonNull Column column) {
		super(column);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new DateDistanceAggregator(getTimeUnit());
	}
}
