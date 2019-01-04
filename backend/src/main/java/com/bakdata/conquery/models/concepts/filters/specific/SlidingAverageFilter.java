package com.bakdata.conquery.models.concepts.filters.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.query.aggregators.filter.SlidingAverageFilterNode;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

@CPSType(id="SLIDING_AVERAGE", base= Filter.class)
public class SlidingAverageFilter extends SlidingSumFilter {

	private static final long serialVersionUID = 1L;

	@Override public FilterNode createAggregator(FilterValue.CQRealRangeFilter filterValue) {
		return new SlidingAverageFilterNode(this, filterValue);
	}
}
