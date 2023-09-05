package com.bakdata.conquery.sql.conversion.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.datasets.concepts.filters.specific.MultiSelectFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConceptFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.Filters;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter.MultiSelectCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSelect;

public class MultiSelectFilterConverter implements FilterConverter<String[], MultiSelectFilter> {

	@Override
	public ConceptFilter convert(MultiSelectFilter multiSelectFilter, FilterContext<String[]> context) {

		ConquerySelect rootSelect = new ExtractingSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING),
				multiSelectFilter.getColumn().getName(),
				String.class
		);

		FilterCondition condition = new MultiSelectCondition(
				context.getConceptTables().qualifyOnPredecessorTableName(CteStep.EVENT_FILTER, rootSelect.alias()),
				context.getValue(),
				context.getParentContext().getSqlDialect().getFunction()
		);

		return new ConceptFilter(
				SqlSelects.builder()
						  .forPreprocessingStep(List.of(rootSelect))
						  .build(),
				Filters.builder()
					   .event(List.of(condition))
					   .build()
		);
	}

	@Override
	public Set<CteStep> requiredSteps() {
		Set<CteStep> multiSelectFilterSteps = new HashSet<>(FilterConverter.super.requiredSteps());
		multiSelectFilterSteps.add(CteStep.EVENT_FILTER);
		return multiSelectFilterSteps;

	}

	@Override
	public Class<? extends MultiSelectFilter> getConversionClass() {
		return MultiSelectFilter.class;
	}

}
