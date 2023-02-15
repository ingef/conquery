package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 *
 * This Filter can use optional labels or a template for displaying, same as {@link MultiSelectFilter}.
 * However, the frontend will fetch and display data beyond the  defined values for {@link BigMultiSelectFilter}/BIG_MULTI_SELECT.
 */
@Getter
@Setter
@CPSType(id = "BIG_MULTI_SELECT", base = Filter.class)
public class BigMultiSelectFilter extends SelectFilter<String[]> {

	@JsonIgnore
	@Override
	public String getFilterType() {
		return FrontendFilterType.Fields.BIG_MULTI_SELECT;
	}

	@Override
	public FilterNode createFilterNode(String[] value) {
		return new MultiSelectFilterNode(getColumn(), value);
	}

	@NotNull
	protected List<FrontendValue> collectLabels() {
		// Frontend expects no Labels when encountering BIG_MULTI_SELECT
		return Collections.emptyList();
	}
}
