package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.util.Set;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.DateDistanceFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.select.DateDistanceSqlAggregator;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;

public class DateDistanceFilterConverter implements FilterConverter<Range.LongRange, DateDistanceFilter> {

	private final DateNowSupplier dateNowSupplier;

	public DateDistanceFilterConverter(DateNowSupplier dateNowSupplier) {
		this.dateNowSupplier = dateNowSupplier;
	}

	@Override
	public SqlFilters convert(DateDistanceFilter dateDistanceFilter, FilterContext<Range.LongRange> context) {
		return DateDistanceSqlAggregator.create(dateDistanceFilter, context, dateNowSupplier).getSqlFilters();
	}

	@Override
	public Set<ConceptCteStep> requiredSteps() {
		return ConceptCteStep.withOptionalSteps(ConceptCteStep.EVENT_FILTER);
	}

	@Override
	public Class<DateDistanceFilter> getConversionClass() {
		return DateDistanceFilter.class;
	}

}
