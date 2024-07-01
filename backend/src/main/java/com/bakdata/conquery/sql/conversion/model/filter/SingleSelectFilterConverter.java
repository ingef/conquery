package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.SingleSelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;

public class SingleSelectFilterConverter extends AbstractSelectFilterConverter<SingleSelectFilter, String> {

	@Override
	protected String[] getValues(FilterContext<String> filterContext) {
		return new String[]{filterContext.getValue()};
	}
}
