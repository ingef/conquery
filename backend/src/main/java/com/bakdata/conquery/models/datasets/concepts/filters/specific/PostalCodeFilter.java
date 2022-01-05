package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;


@CPSType(id = "POSTAL_CODE", base = Filter.class)
public class PostalCodeFilter extends SingleColumnFilter<FilterValue.PostalCodeFilterValue> {

	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.POSTAL_CODE);
	}

	@Override
	public FilterNode<?> createFilterNode(FilterValue.PostalCodeFilterValue postalCodeFilterValue) {
		return new MultiSelectFilterNode(getColumn(), postalCodeFilterValue.getResolvedValue());
	}

}