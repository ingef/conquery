package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.Set;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.select.CountSqlAggregator;

public class CountFilterConverter implements FilterConverter<Range.LongRange, CountFilter> {

	@Override
	public SqlFilters convert(CountFilter countFilter, FilterContext<Range.LongRange> context) {
		return CountSqlAggregator.create(countFilter, context).getSqlFilters();
	}

	@Override
	public Set<ConceptCteStep> requiredSteps() {
		return ConceptCteStep.withOptionalSteps(ConceptCteStep.AGGREGATION_FILTER);
	}

	@Override
	public Class<CountFilter> getConversionClass() {
		return CountFilter.class;
	}

}
