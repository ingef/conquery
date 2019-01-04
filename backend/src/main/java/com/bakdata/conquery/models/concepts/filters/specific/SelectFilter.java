package com.bakdata.conquery.models.concepts.filters.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.query.aggregators.filter.SelectFilterNode;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQSelectFilter;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

import lombok.Getter;
import lombok.Setter;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@Getter @Setter
@CPSType(id="SINGLE_SELECT", base= Filter.class)
public class SelectFilter extends AbstractSelectFilter<CQSelectFilter> implements ISelectFilter {

	private static final long serialVersionUID = 1L;
	
	public SelectFilter() {
		super(128, FEFilterType.SELECT);
	}
	
	@Override
	public FilterNode createAggregator(CQSelectFilter filterValue) {
		return new SelectFilterNode(this, filterValue);
	}
}
