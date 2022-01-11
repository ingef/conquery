package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Setter;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@Setter
@CPSType(id = "SELECT", base = Filter.class)
public class MultiSelectFilter extends AbstractSelectFilter {

	private static final int MAXIMUM_SIZE = 128;


	@Override
	public FilterNode<?> createFilterNode(String[] value) {
		return new MultiSelectFilterNode(getColumn(), value);
	}

	@JsonIgnore
	private boolean isBig() {
		return getValues().size() > MAXIMUM_SIZE;
	}

	public Class<? extends FilterValue<String[]>> getFilterType() {
		// Big MultiSelects are rendered differently in frontend
		if (isBig()) {
			return FilterValue.CQBigMultiSelectFilter.class;
		}

		return FilterValue.CQMultiSelectFilter.class;
	}

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		// For normal MultiSelect, we simply push the configured data to the frontend.
		if (values != null && !values.isEmpty() && !isBig()) {
			f.setOptions(
					getValues().stream()
							   .map(v -> new FEValue(getLabelFor(v), v))
							   .collect(Collectors.toList())
			);
		}

	}
}
