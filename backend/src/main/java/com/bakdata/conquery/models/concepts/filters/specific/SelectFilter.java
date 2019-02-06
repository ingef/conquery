package com.bakdata.conquery.models.concepts.filters.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQSelectFilter;
import com.bakdata.conquery.models.query.filter.event.SelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.Setter;

/**
 * This filter represents a selectId in the front end. This means that the user can selectId one or more values from a list of values.
 */
@Getter
@Setter
@CPSType(id = "SINGLE_SELECT", base = Filter.class)
public class SelectFilter extends AbstractSelectFilter<CQSelectFilter> implements ISelectFilter {

	

	public SelectFilter() {
		super(128, FEFilterType.SELECT);
	}

	@Override
	public FilterNode<?, ?> createFilter(CQSelectFilter filterValue, Aggregator<?> aggregator) {
		return new SelectFilterNode(this, filterValue);
	}
}
