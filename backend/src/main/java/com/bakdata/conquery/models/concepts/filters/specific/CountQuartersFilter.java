package com.bakdata.conquery.models.concepts.filters.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.date.CountQuartersAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
@CPSType(id="COUNT_QUARTERS", base=Filter.class)
public class CountQuartersFilter extends Filter<Range.LongRange> {

	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.INTEGER_RANGE);
		f.setMin(1);
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[0];
	}

	@Override
	public FilterNode createAggregator(Range.LongRange value) {
		return new RangeFilterNode(value, new CountQuartersAggregator());
	}
}
