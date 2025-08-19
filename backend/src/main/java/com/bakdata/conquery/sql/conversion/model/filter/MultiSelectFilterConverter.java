package com.bakdata.conquery.sql.conversion.model.filter;

import java.util.Set;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.MultiSelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;

public class MultiSelectFilterConverter extends AbstractSelectFilterConverter<MultiSelectFilter, Set<String>> {

	@Override
	protected String[] getValues(FilterContext<Set<String>> filterContext) {
		return filterContext.getValue().toArray(String[]::new);
	}
}
