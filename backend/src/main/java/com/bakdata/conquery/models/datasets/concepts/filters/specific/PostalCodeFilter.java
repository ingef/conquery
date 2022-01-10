package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.postalcode.PostalCodeSearchEntity;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;


@CPSType(id = "POSTAL_CODE", base = Filter.class)
public class PostalCodeFilter extends SingleColumnFilter<PostalCodeSearchEntity> {

	@Override
	public void configureFrontend(FEFilter f) {

	}

	@Override
	public FilterNode<?> createFilterNode(PostalCodeSearchEntity postalCodeSearchEntity) {
		return new MultiSelectFilterNode(getColumn(), postalCodeSearchEntity.getResolvedValue());
	}

	@Override
	public Class<FilterValue.PostalCodeFilterValue> getFilterType() {
		return FilterValue.PostalCodeFilterValue.class;
	}

}