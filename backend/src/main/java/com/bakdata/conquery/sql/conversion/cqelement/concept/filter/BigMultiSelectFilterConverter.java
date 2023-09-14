package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.Set;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.BigMultiSelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;

public class BigMultiSelectFilterConverter implements FilterConverter<String[], BigMultiSelectFilter> {

	@Override
	public ConceptFilter convert(BigMultiSelectFilter bigMultiSelectFilter, FilterContext<String[]> context) {
		return SelectFilterUtil.convert(bigMultiSelectFilter, context, context.getValue());
	}

	@Override
	public Set<CteStep> requiredSteps() {
		return CteStep.withOptionalSteps(CteStep.EVENT_FILTER);
	}

	@Override
	public Class<? extends BigMultiSelectFilter> getConversionClass() {
		return BigMultiSelectFilter.class;
	}

}
