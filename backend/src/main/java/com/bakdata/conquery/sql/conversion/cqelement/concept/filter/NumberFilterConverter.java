package com.bakdata.conquery.sql.conversion.cqelement.concept.filter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.NumberFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.filter.NumberCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Field;

public class NumberFilterConverter implements FilterConverter<IRange<? extends Number, ?>, NumberFilter<IRange<? extends Number, ?>>> {

	private static final Class<? extends NumberFilter> CLASS = NumberFilter.class;

	@Override
	public ConceptFilter convert(NumberFilter<IRange<? extends Number, ?>> numberFilter, FilterContext<IRange<? extends Number, ?>> context) {

		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(numberFilter.getColumn().getType());

		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessor(ConceptCteStep.PREPROCESSING),
				numberFilter.getColumn().getName(),
				numberClass
		);


		Field<Number> eventFilterCtePredecessor = context.getConceptTables().qualifyOnPredecessor(ConceptCteStep.EVENT_FILTER, rootSelect.aliased());
		IRange<? extends Number, ?> filterValue = prepareFilterValue(numberFilter, context);
		NumberCondition condition = new NumberCondition(eventFilterCtePredecessor, filterValue);

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

	/**
	 * If there is a long range filter on a column of type MONEY, the filter value will represent a decimal with the point moved right 2 places right.
	 * <p>
	 * For example, the filter value {@code {min: 1000ct, max: 2000ct}} will be converted to {@code {min: 10,00€, max: 20,00€}}
	 */
	private static IRange<? extends Number, ?> prepareFilterValue(
			NumberFilter<IRange<? extends Number, ?>> numberFilter,
			FilterContext<IRange<? extends Number, ?>> context
	) {
		IRange<? extends Number, ?> value = context.getValue();
		if (numberFilter.getColumn().getType() != MajorTypeId.MONEY || !(context.getValue() instanceof Range.LongRange)) {
			return value;
		}
		Long min = (Long) value.getMin();
		Long max = (Long) value.getMax();
		return Range.LongRange.of(
				BigDecimal.valueOf(min).movePointLeft(2),
				BigDecimal.valueOf(max).movePointLeft(2)
		);
	}

}
