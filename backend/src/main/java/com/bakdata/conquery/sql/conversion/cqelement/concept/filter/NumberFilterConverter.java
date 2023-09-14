package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.NumberFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.bakdata.conquery.sql.conversion.model.filter.NumberCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;

public class NumberFilterConverter implements FilterConverter<IRange<? extends Number, ?>, NumberFilter<IRange<? extends Number, ?>>> {

	private static final Class<? extends NumberFilter> CLASS = NumberFilter.class;

	@Override
	public ConceptFilter convert(NumberFilter<IRange<? extends Number, ?>> numberFilter, FilterContext<IRange<? extends Number, ?>> context) {

		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(numberFilter.getColumn().getType());

		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING),
				numberFilter.getColumn().getName(),
				numberClass
		);

		NumberCondition condition = new NumberCondition(
				context.getConceptTables().qualifyOnPredecessorTableName(CteStep.EVENT_FILTER, rootSelect.aliased()),
				context.getValue()
		);

		return new ConceptFilter(
				SqlSelects.builder()
						  .forPreprocessingStep(Collections.singletonList(rootSelect))
						  .build(),
				Filters.builder()
					   .event(Collections.singletonList(condition))
					   .build()
		);
	}

	@Override
	public Set<CteStep> requiredSteps() {
		return CteStep.withOptionalSteps(CteStep.EVENT_FILTER);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends NumberFilter<IRange<? extends Number, ?>>> getConversionClass() {
		return (Class<? extends NumberFilter<IRange<? extends Number, ?>>>) CLASS;
	}

}
