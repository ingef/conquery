package com.bakdata.conquery.models.concepts.filters.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.date.DurationSumAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@CPSType(id = "DURATION_SUM", base = Filter.class)
public class DurationSumFilter extends Filter<Range.LongRange> {

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setType(FEFilterType.INTEGER_RANGE);
		f.setMin(0);
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[0];
	}

	@Override
	public FilterNode createAggregator(Range.LongRange value) {
		return new RangeFilterNode(value, new DurationSumAggregator());
	}
}
