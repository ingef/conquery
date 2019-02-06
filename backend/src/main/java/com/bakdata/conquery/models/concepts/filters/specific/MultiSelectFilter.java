package com.bakdata.conquery.models.concepts.filters.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQMultiSelectFilter;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import lombok.Getter;
import lombok.Setter;

/**
 * This filter represents a selectId in the front end. This means that the user can selectId one or more values from a list of values.
 */
@Getter
@Setter
@CPSType(id = "SELECT", base = Filter.class)
public class MultiSelectFilter extends AbstractSelectFilter<CQMultiSelectFilter> implements ISelectFilter {

	

	public MultiSelectFilter() {
		super(128, FEFilterType.MULTI_SELECT);
	}

	@Override
	public MultiSelectFilterNode<MultiSelectFilter> createFilter(CQMultiSelectFilter filterValue, Aggregator<?> aggregator) {
		return new MultiSelectFilterNode<>(this, filterValue);
	}
}
