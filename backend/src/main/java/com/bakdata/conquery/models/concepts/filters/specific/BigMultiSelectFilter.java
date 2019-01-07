package com.bakdata.conquery.models.concepts.filters.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQMultiSelectFilter;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.zigurs.karlis.utils.search.QuickSearch;

import lombok.Getter;
import lombok.Setter;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@Getter
@Setter
@CPSType(id = "BIG_MULTI_SELECT", base = Filter.class)
public class BigMultiSelectFilter extends AbstractSelectFilter<CQMultiSelectFilter> implements ISelectFilter {

	

	public BigMultiSelectFilter() {
		super(-1, FEFilterType.BIG_MULTI_SELECT);
	}

	@Override
	public FilterNode createAggregator(CQMultiSelectFilter filterValue) {
		//		if (filterValue.getValue().length == 1) {
		//			return new ValueAboveZeroFilterNode<>(this, filterValue, new SelectAggregator(getColumn(), filterValue.getValue()[0]));
		//		}


		return new MultiSelectFilterNode(this, filterValue);
	}
}
