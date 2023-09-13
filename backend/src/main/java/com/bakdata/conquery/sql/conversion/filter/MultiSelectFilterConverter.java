package com.bakdata.conquery.sql.conversion.filter;

import java.util.Set;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.MultiSelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConceptFilter;

public class MultiSelectFilterConverter implements FilterConverter<String[], MultiSelectFilter> {

	@Override
	public ConceptFilter convert(MultiSelectFilter multiSelectFilter, FilterContext<String[]> context) {
		return SelectFilterUtil.convert(multiSelectFilter, context, context.getValue());
	}

	@Override
	public Set<CteStep> requiredSteps() {
		return CteStep.withOptionalSteps(CteStep.EVENT_FILTER);
	}

	@Override
	public Class<? extends MultiSelectFilter> getConversionClass() {
		return MultiSelectFilter.class;
	}

}
