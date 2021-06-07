package com.bakdata.conquery.models.concepts.filters.event;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.Setter;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@Getter
@Setter
@CPSType(id = "SELECT", base = Filter.class)
public class MultiSelectFilter extends AbstractSelectFilter<String[]> {

	public MultiSelectFilter() {
		super(128, FEFilterType.MULTI_SELECT);
	}

	@Override
	public EventFilterNode createEventFilter(String[] value) {
		return new MultiSelectFilterNode(getColumn(), value);
	}
}
