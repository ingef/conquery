package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.Set;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectFilterUtil;
import com.bakdata.conquery.sql.conversion.model.filter.MultiSelectCondition;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jooq.Condition;

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
	public FilterNode<?> createFilterNode(Set<String> value) {
		return new MultiSelectFilterNode(getColumn(), value);
	}

	@Override
	public SqlFilters convertToSqlFilter(FilterContext<Set<String>> filterContext) {
		return SelectFilterUtil.convert(this, filterContext, filterContext.getValue().toArray(String[]::new));
	}

	@Override
	public Condition convertForTableExport(FilterContext<Set<String>> filterContext) {
		return MultiSelectCondition.onColumn(getColumn(), filterContext.getValue().toArray(String[]::new), filterContext.getSqlDialect().getFunctionProvider()).condition();
	}
}
