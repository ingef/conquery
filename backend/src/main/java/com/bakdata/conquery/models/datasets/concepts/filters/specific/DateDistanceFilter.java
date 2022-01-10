package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
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
public class DateDistanceFilter extends SingleColumnFilter<Range.LongRange> {

	@NotNull
	private ChronoUnit timeUnit = ChronoUnit.YEARS;
	
	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE);
	}

	@Override
	public Class<? extends FilterValue<? extends Range.LongRange>> getFilterType() {
		return FilterValue.CQIntegerRangeFilter.class;
	}

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {

	}
	
	@Override
	public FilterNode createFilterNode(Range.LongRange value) {
		return new DateDistanceFilterNode(getColumn(), timeUnit, value);
	}
}