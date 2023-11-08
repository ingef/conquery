package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.filter.SumCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.SumSqlSelect;
import org.jooq.Field;

public class SumFilterConverter implements FilterConverter<IRange<? extends Number, ?>, SumFilter<IRange<? extends Number, ?>>> {

	private static final Class<? extends SumFilter> CLASS = SumFilter.class;

	@Override
	public ConceptFilter convert(SumFilter<IRange<? extends Number, ?>> sumFilter, FilterContext<IRange<? extends Number, ?>> context) {

		// TODO(tm): convert getSubtractColumn and getDistinctByColumn
		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumFilter.getColumn().getType());
		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessor(ConceptStep.PREPROCESSING),
				sumFilter.getColumn().getName(),
				numberClass
		);

		Field<? extends Number> qualifiedRootSelect = context.getConceptTables()
															 .qualifyOnPredecessor(ConceptStep.AGGREGATION_SELECT, rootSelect.aliased());
		SumSqlSelect sumSqlSelect = new SumSqlSelect(qualifiedRootSelect, sumFilter.getName());

		Field<? extends Number> qualifiedSumGroupBy = context.getConceptTables()
															 .qualifyOnPredecessor(ConceptStep.AGGREGATION_FILTER, sumSqlSelect.aliased());
		SumCondition sumFilterCondition = new SumCondition(qualifiedSumGroupBy, context.getValue());

		return new ConceptFilter(
				SqlSelects.builder()
						  .forPreprocessingStep(List.of(rootSelect))
						  .forAggregationSelectStep(List.of(sumSqlSelect))
						  .build(),
				Filters.builder()
					   .group(List.of(sumFilterCondition))
					   .build()
		);
	}

	@Override
	public Set<ConceptStep> requiredSteps() {
		return ConceptStep.withOptionalSteps(ConceptStep.AGGREGATION_FILTER);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends SumFilter<IRange<? extends Number, ?>>> getConversionClass() {
		return (Class<? extends SumFilter<IRange<? extends Number, ?>>>) CLASS;
	}

}
