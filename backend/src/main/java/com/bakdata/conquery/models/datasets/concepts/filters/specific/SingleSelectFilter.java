package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.Set;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.query.filter.event.SelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectFilterUtil;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import net.minidev.json.annotate.JsonIgnore;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.",
 *
 * @jsonExample {"label":"gender","column":"reference_data.gender","type":"SINGLE_SELECT"}
 */
@CPSType(id = "SINGLE_SELECT", base = Filter.class)
public class SingleSelectFilter extends SelectFilter<String> {

	@Override
	public FilterNode<?> createFilterNode(String value) {
		return new SelectFilterNode(getColumn(), value);
	}

	@Override
	@JsonIgnore
	public String getFilterType() {
		return FrontendFilterType.Fields.SELECT;
	}

	@Override
	public SqlFilters convertToSqlFilter(FilterContext<String> filterContext) {
		return SelectFilterUtil.convert(this, filterContext, new String[]{filterContext.getValue()});
	}

	@Override
	public Set<ConceptCteStep> getRequiredSqlSteps() {
		return ConceptCteStep.withOptionalSteps(ConceptCteStep.EVENT_FILTER);
	}

}
