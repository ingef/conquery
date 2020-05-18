package com.bakdata.conquery.models.concepts.filters.specific;

import java.time.temporal.ChronoUnit;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.filter.event.DateDistanceFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@Getter @Setter @Slf4j
@CPSType(id="DATE_DISTANCE", base=Filter.class)
public class DateDistanceFilter extends Filter<Range.LongRange> {

	@NotNull
	private ChronoUnit timeUnit = ChronoUnit.YEARS;

	@Override
	public void configureFrontend(FEFilter f) { }

	@Override
	public Column[] getRequiredColumns() {
		return new Column[0];
	}

	@Override
	public FilterNode createAggregator(Range.LongRange value) {
		return new DateDistanceFilterNode(timeUnit, value);
	}
}