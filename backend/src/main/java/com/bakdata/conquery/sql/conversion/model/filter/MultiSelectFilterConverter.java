package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.MultiSelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;

public class MultiSelectFilterConverter extends AbstractSelectFilterConverter<MultiSelectFilter, String[]> {

	@Override
	protected String[] getValues(FilterContext<String[]> filterContext) {
		return filterContext.getValue();
	}
}
