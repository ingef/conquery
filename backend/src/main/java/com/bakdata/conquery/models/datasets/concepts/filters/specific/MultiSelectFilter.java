package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.Set;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.filter.event.SubstringMultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.MultiSelectFilterConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@CPSType(id = "SELECT", base = Filter.class)
public class MultiSelectFilter extends SelectFilter<Set<String>> {

	@JsonIgnore
	@Override
	public String getFilterType() {
		// If we have labels we don't need a big multi select.
		if (!getLabels().isEmpty()) {
			return FrontendFilterType.Fields.MULTI_SELECT;
		}

		return FrontendFilterType.Fields.BIG_MULTI_SELECT;

	}

	@Override
	public FilterNode<Set<String>> createFilterNode(Set<String> value) {
		if (getSubstring() != null && !getSubstring().isAll()) {
			return new SubstringMultiSelectFilterNode(getColumn().resolve(), value, getSubstring());
		}

		return new MultiSelectFilterNode(getColumn().resolve(), value);
	}

	@Override
	public FilterConverter<MultiSelectFilter, Set<String>> createConverter() {
		return new MultiSelectFilterConverter();
	}
}
