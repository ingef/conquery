package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.BigMultiSelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;

public class BigMultiSelectFilterConverter extends AbstractSelectFilterConverter<BigMultiSelectFilter, String[]> {

	@Override
	protected String[] getValues(FilterContext<String[]> filterContext) {
		return filterContext.getValue();
	}
}
