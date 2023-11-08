package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.Set;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.SingleSelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;

public class SingleSelectFilterConverter implements FilterConverter<String, SingleSelectFilter> {

	@Override
	public ConceptFilter convert(SingleSelectFilter singleSelect, FilterContext<String> context) {
		return SelectFilterUtil.convert(singleSelect, context, new String[]{context.getValue()});
	}

	@Override
	public Set<ConceptCteStep> requiredSteps() {
		return ConceptCteStep.withOptionalSteps(ConceptCteStep.EVENT_FILTER);
	}

	@Override
	public Class<? extends SingleSelectFilter> getConversionClass() {
		return SingleSelectFilter.class;
	}

}
