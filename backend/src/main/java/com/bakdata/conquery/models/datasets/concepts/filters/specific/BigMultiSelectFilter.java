package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.Set;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.MultiSelectFilterConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 * <p>
 * This Filter can use optional labels or a template for displaying, same as {@link MultiSelectFilter}.
 * However, the frontend will fetch and display data beyond the  defined values for {@link BigMultiSelectFilter}/BIG_MULTI_SELECT.
 */
@Getter
@Setter
@CPSType(id = "BIG_MULTI_SELECT", base = Filter.class)
public class BigMultiSelectFilter extends SelectFilter<Set<String>> {

	@JsonIgnore
	@Override
	public String getFilterType() {
		return FrontendFilterType.Fields.BIG_MULTI_SELECT;
	}

	@Override
	public FilterNode createFilterNode(Set<String> value) {
		return new MultiSelectFilterNode(getColumn().resolve(), value);
	}

	@Override
	public FilterConverter<MultiSelectFilter, Set<String>> createConverter() {
		return new MultiSelectFilterConverter();
	}
}
