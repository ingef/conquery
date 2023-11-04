package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.NumberFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.filter.NumberCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;

public class NumberFilterConverter implements FilterConverter<IRange<? extends Number, ?>, NumberFilter<IRange<? extends Number, ?>>> {

	private static final Class<? extends NumberFilter> CLASS = NumberFilter.class;

	@Override
	public ConceptFilter convert(NumberFilter<IRange<? extends Number, ?>> numberFilter, FilterContext<IRange<? extends Number, ?>> context) {

		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(numberFilter.getColumn().getType());

		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.PREPROCESSING),
				numberFilter.getColumn().getName(),
				numberClass
		);

		NumberCondition condition = new NumberCondition(
				context.getConceptTables().qualifyOnPredecessor(ConceptCteStep.EVENT_FILTER, rootSelect.aliased()),
				context.getValue()
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
	public Set<ConceptCteStep> requiredSteps() {
		return ConceptCteStep.withOptionalSteps(ConceptCteStep.EVENT_FILTER);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends NumberFilter<IRange<? extends Number, ?>>> getConversionClass() {
		return (Class<? extends NumberFilter<IRange<? extends Number, ?>>>) CLASS;
	}

}
