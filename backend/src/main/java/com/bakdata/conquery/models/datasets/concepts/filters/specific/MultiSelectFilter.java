package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@CPSType(id = "SELECT", base = Filter.class)
public class MultiSelectFilter extends SelectFilter<String[]> {

	@JsonIgnore
	@Override
	public String getFilterType() {
		// If we have labels we don't need a big multi select.
		if (!getLabels().isEmpty()) {
			return FEFilterType.Fields.MULTI_SELECT;
		}

		return FEFilterType.Fields.BIG_MULTI_SELECT;

	}

	@Override
	public FilterNode<?> createFilterNode(String[] value) {
		return new MultiSelectFilterNode(getColumn(), value);
	}


	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context, String[] selected) {
		return null; //TODO requires index to be located at CBlock Level to dereference selected at Import level, unless we want to store per Entity the strings (which would be a lot of wasted memory)
	}
}
