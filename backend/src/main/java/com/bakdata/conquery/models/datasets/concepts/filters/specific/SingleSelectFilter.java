package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.Collections;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.filter.event.SubstringMultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.SingleSelectFilterConverter;
import net.minidev.json.annotate.JsonIgnore;

/**
 * This filter represents a select in the front end. This means that the user can select exactly one or value from a list of values.",
 *
 * @jsonExample {"label":"gender","column":"reference_data.gender","type":"SINGLE_SELECT"}
 */
@CPSType(id = "SINGLE_SELECT", base = Filter.class)
public class SingleSelectFilter extends SelectFilter<String> {

	@Override
	public FilterNode<?> createFilterNode(String value) {
		if (getSubstring() != null && !getSubstring().isAll()) {
			return new SubstringMultiSelectFilterNode(getColumn().resolve(), Collections.singleton(value), getSubstring());
		}

		return new MultiSelectFilterNode(getColumn().resolve(), Collections.singleton(value));
	}

	@Override
	@JsonIgnore
	public String getFilterType() {
		return FrontendFilterType.Fields.SELECT;
	}

	@Override
	public FilterConverter<SingleSelectFilter, String> createConverter() {
		return new SingleSelectFilterConverter();
	}
}
