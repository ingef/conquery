package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.Set;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.select.SumSqlAggregator;

public class SumFilterConverter implements FilterConverter<IRange<? extends Number, ?>, SumFilter<IRange<? extends Number, ?>>> {

	private static final Class<? extends SumFilter> CLASS = SumFilter.class;

	@Override
	public SqlFilters convert(SumFilter<IRange<? extends Number, ?>> sumFilter, FilterContext<IRange<? extends Number, ?>> context) {
		return SumSqlAggregator.create(sumFilter, context).getSqlFilters();
	}

	@Override
	public Set<ConceptCteStep> requiredSteps() {
		return ConceptCteStep.withOptionalSteps(ConceptCteStep.AGGREGATION_FILTER);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends SumFilter<IRange<? extends Number, ?>>> getConversionClass() {
		return (Class<? extends SumFilter<IRange<? extends Number, ?>>>) CLASS;
	}

}
