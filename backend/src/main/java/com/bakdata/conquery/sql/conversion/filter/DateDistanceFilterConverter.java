package com.bakdata.conquery.sql.conversion.filter;

import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.DateDistanceFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConceptFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.Filters;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter.DateDistanceCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.DateDistanceSelect;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;

public class DateDistanceFilterConverter implements FilterConverter<Range.LongRange, DateDistanceFilter> {

	private final DateNowSupplier dateNowSupplier;

	public DateDistanceFilterConverter(DateNowSupplier dateNowSupplier) {
		this.dateNowSupplier = dateNowSupplier;
	}

	@Override
	public ConceptFilter convert(DateDistanceFilter dateDistanceFilter, FilterContext<Range.LongRange> context) {

		DateDistanceSelect dateDistanceSelect = new DateDistanceSelect(
				dateNowSupplier,
				dateDistanceFilter.getTimeUnit(),
				context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING),
				dateDistanceFilter.getColumn(),
				dateDistanceFilter.getName(),
				context.getParentContext().getDateRestrictionRange(),
				context.getParentContext().getSqlDialect().getFunction()
		);

		FilterCondition dateDistanceCondition = new DateDistanceCondition(
				context.getConceptTables().qualifyOnPredecessorTableName(CteStep.EVENT_FILTER, dateDistanceSelect.aliased()),
				context.getValue()
		);

		return new ConceptFilter(
				SqlSelects.builder()
						  .forPreprocessingStep(Collections.singletonList(dateDistanceSelect))
						  .build(),
				Filters.builder()
					   .event(Collections.singletonList(dateDistanceCondition))
					   .build()
		);
	}

	@Override
	public Set<CteStep> requiredSteps() {
		return CteStep.withOptionalSteps(CteStep.EVENT_FILTER);
	}

	@Override
	public Class<DateDistanceFilter> getConversionClass() {
		return DateDistanceFilter.class;
	}

}
