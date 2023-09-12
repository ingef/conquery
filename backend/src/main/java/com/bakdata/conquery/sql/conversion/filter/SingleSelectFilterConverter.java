package com.bakdata.conquery.sql.conversion.filter;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.SingleSelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConceptFilter;

public class SingleSelectFilterConverter implements FilterConverter<String, SingleSelectFilter> {

	@Override
	public ConceptFilter convert(SingleSelectFilter singleSelect, FilterContext<String> context) {
		return SelectFilterUtil.convert(singleSelect, context, String.class);
	}

	@Override
	public Set<CteStep> requiredSteps() {
		Set<CteStep> multiSelectFilterSteps = new HashSet<>(FilterConverter.super.requiredSteps());
		multiSelectFilterSteps.add(CteStep.EVENT_FILTER);
		return multiSelectFilterSteps;
	}

	@Override
	public Class<? extends SingleSelectFilter> getConversionClass() {
		return SingleSelectFilter.class;
	}

}
