package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectFilterUtil;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

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

	@Override
	public SqlFilters convertToSqlFilter(FilterContext<String[]> filterContext) {
		return SelectFilterUtil.convert(this, filterContext, filterContext.getValue());
	}

}
