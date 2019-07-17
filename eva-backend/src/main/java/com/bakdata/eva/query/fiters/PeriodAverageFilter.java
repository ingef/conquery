package com.bakdata.eva.query.fiters;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.eva.query.aggregators.PeriodAverageAggregator;

@CPSType(id = "PERIOD_AVERAGE", base = Filter.class)
public class PeriodAverageFilter extends SingleColumnFilter<Range<BigDecimal>> {

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setType(FEFilterType.REAL_RANGE);
	}

	@Override
	public FilterNode<?> createAggregator(Range<BigDecimal> filterValue) {
		return new RangeFilterNode<>(Range.DoubleRange.fromNumberRange(filterValue), new PeriodAverageAggregator(getColumn()));
	}
}
