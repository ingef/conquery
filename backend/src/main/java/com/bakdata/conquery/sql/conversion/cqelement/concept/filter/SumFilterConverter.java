package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.bakdata.conquery.sql.conversion.model.filter.SumCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SumSqlSelect;
import org.jooq.Field;

public class SumFilterConverter implements FilterConverter<IRange<? extends Number, ?>, SumFilter<IRange<? extends Number, ?>>> {

	private static final Class<? extends SumFilter> CLASS = SumFilter.class;

	@Override
	public ConceptFilter convert(SumFilter<IRange<? extends Number, ?>> sumFilter, FilterContext<IRange<? extends Number, ?>> context) {

		// TODO(tm): convert getSubtractColumn and getDistinctByColumn
		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumFilter.getColumn().getType());
		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING),
				sumFilter.getColumn().getName(),
				numberClass
		);

		Field<? extends Number> qualifiedRootSelect = context.getConceptTables()
															 .qualifyOnPredecessorTableName(CteStep.AGGREGATION_SELECT, rootSelect.aliased());
		SumSqlSelect sumSqlSelect = new SumSqlSelect(qualifiedRootSelect, sumFilter.getName());

		Field<? extends Number> qualifiedSumGroupBy = context.getConceptTables()
															 .qualifyOnPredecessorTableName(CteStep.AGGREGATION_FILTER, sumSqlSelect.aliased());
		SumCondition sumFilterCondition = new SumCondition(qualifiedSumGroupBy, context.getValue());

		return new ConceptFilter(
				SqlSelects.builder()
						  .forPreprocessingStep(Collections.singletonList(rootSelect))
						  .forAggregationSelectStep(Collections.singletonList(sumSqlSelect))
						  .build(),
				Filters.builder()
					   .group(Collections.singletonList(sumFilterCondition))
					   .build()
		);
	}

	@Override
	public Set<CteStep> requiredSteps() {
		return CteStep.withOptionalSteps(CteStep.AGGREGATION_FILTER);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends SumFilter<IRange<? extends Number, ?>>> getConversionClass() {
		return (Class<? extends SumFilter<IRange<? extends Number, ?>>>) CLASS;
	}

}
