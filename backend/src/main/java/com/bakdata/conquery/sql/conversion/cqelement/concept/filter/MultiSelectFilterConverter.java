package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.Set;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.MultiSelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;

public class MultiSelectFilterConverter implements FilterConverter<String[], MultiSelectFilter> {

	@Override
	public ConceptFilter convert(MultiSelectFilter multiSelectFilter, FilterContext<String[]> context) {
		return SelectFilterUtil.convert(multiSelectFilter, context, context.getValue());
	}

	@Override
	public Set<ConceptStep> requiredSteps() {
		return ConceptStep.withOptionalSteps(ConceptStep.EVENT_FILTER);
	}

	@Override
	public Class<? extends MultiSelectFilter> getConversionClass() {
		return MultiSelectFilter.class;
	}

}
