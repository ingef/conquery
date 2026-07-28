package com.bakdata.conquery.sql.conversion.model.filter;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;

public class MultiSelectFilterConverter extends AbstractSelectFilterConverter {

	@Override
	protected String[] getValues(FilterContext<Set<String>> filterContext) {
		return filterContext.getValue().toArray(String[]::new);
	}
}
