package com.bakdata.conquery.models.concepts.filters.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;

import lombok.Getter;
import lombok.Setter;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@Getter
@Setter
@CPSType(id = "SELECT", base = Filter.class)
public class MultiSelectFilter extends AbstractSelectFilter<String[]> implements ISelectFilter {

	

	public MultiSelectFilter() {
		super(128, FEFilterType.MULTI_SELECT);
	}

	@Override
	public MultiSelectFilterNode<MultiSelectFilter> createAggregator(FilterValue<String[]> filterValue) {
		return new MultiSelectFilterNode<>(this, filterValue.getValue());
	}
}
