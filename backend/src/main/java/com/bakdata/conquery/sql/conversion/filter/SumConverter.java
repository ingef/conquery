package com.bakdata.conquery.sql.conversion.filter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConceptFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.Filters;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter.SumCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.SumGroupBy;
import org.jooq.Field;

public class SumConverter implements FilterConverter<IRange<? extends Number, ?>, SumFilter<IRange<? extends Number, ?>>> {

	private static final Class<? extends SumFilter> CLASS = SumFilter.class;

	@Override
	public ConceptFilter convert(SumFilter<IRange<? extends Number, ?>> sumFilter, FilterContext<IRange<? extends Number, ?>> context) {

		// TODO(tm): convert getSubtractColumn and getDistinctByColumn
		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumFilter.getColumn().getType());
		ExtractingSelect<? extends Number> rootSelect = new ExtractingSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING),
				sumFilter.getColumn().getName(),
				numberClass
		);

		Field<? extends Number> qualifiedRootSelect = context.getConceptTables().qualifyOnPredecessorTableName(CteStep.AGGREGATION_SELECT, rootSelect.alias());
		SumGroupBy sumGroupBy = new SumGroupBy(qualifiedRootSelect);

		Field<? extends Number> qualifiedSumGroupBy = context.getConceptTables().qualifyOnPredecessorTableName(CteStep.AGGREGATION_FILTER, sumGroupBy.alias());
		SumCondition sumFilterCondition = new SumCondition(qualifiedSumGroupBy, context.getValue());

		return new ConceptFilter(
				SqlSelects.builder()
						  .forPreprocessingStep(Collections.singletonList(rootSelect))
						  .forAggregationSelectStep(Collections.singletonList(sumGroupBy))
						  .build(),
				Filters.builder()
					   .group(Collections.singletonList(sumFilterCondition))
					   .build()
		);
	}

	@Override
	public Set<CteStep> requiredSteps() {
		Set<CteStep> sumFilterSteps = new HashSet<>(FilterConverter.super.requiredSteps());
		sumFilterSteps.add(CteStep.AGGREGATION_FILTER);
		return sumFilterSteps;

	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends SumFilter<IRange<? extends Number, ?>>> getConversionClass() {
		return (Class<? extends SumFilter<IRange<? extends Number, ?>>>) CLASS;
	}

}
