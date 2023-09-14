package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jooq.Field;

/**
 * {@link DateAggregationDates} keep track of all validity dates of list of {@link QueryStep}s that need to be aggregated.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DateAggregationDates {

	private static final String RANGE_START = "RANGE_START";
	private static final String RANGE_END = "RANGE_END";
	private static final AtomicInteger validityDateCounter = new AtomicInteger();
	private final List<ColumnDateRange> validityDates;

	public static DateAggregationDates forSingleStep(QueryStep queryStep) {
		List<ColumnDateRange> validityDates = queryStep.getSelects()
													   .getValidityDate()
													   .map(List::of)
													   .orElse(Collections.emptyList());
		return new DateAggregationDates(validityDates);
	}

	public static DateAggregationDates forSteps(List<QueryStep> querySteps) {
		List<ColumnDateRange> validityDates = querySteps.stream()
														.filter(queryStep -> queryStep.getSelects().getValidityDate().isPresent())
														.map(DateAggregationDates::numerateValidityDate)
														.toList();
		return new DateAggregationDates(validityDates);
	}

	public boolean dateAggregationImpossible() {
		return this.validityDates.isEmpty();
	}

	public Field<Date>[] allStarts() {
		return this.validityDates.stream().map(ColumnDateRange::getStart).toArray(Field[]::new);
	}

	public Field<Date>[] allEnds() {
		return this.validityDates.stream().map(ColumnDateRange::getEnd).toArray(Field[]::new);
	}

	public List<SqlSelect> allStartsAndEnds() {
		return this.validityDates.stream()
								 .flatMap(validityDate -> validityDate.toFields().stream())
								 .map(FieldWrapper::new)
								 .collect(Collectors.toList());
	}

	public DateAggregationDates qualify(String qualifier) {
		List<ColumnDateRange> qualified = this.validityDates.stream()
															.map(validityDate -> validityDate.qualify(qualifier))
															.toList();
		// validity dates will already be numerated, no we don't need no apply a counter again
		return new DateAggregationDates(qualified);
	}

	private static ColumnDateRange numerateValidityDate(QueryStep queryStep) {
		ColumnDateRange validityDate = queryStep.getQualifiedSelects().getValidityDate().get();

		if (validityDate.isSingleColumnRange()) {
			return validityDate;
		}

		Field<Date> rangeStart = validityDate.getStart().as("%s_%s".formatted(RANGE_START, validityDateCounter.get()));
		Field<Date> rangeEnd = validityDate.getEnd().as("%s_%s".formatted(RANGE_END, validityDateCounter.getAndIncrement()));

		return ColumnDateRange.of(rangeStart, rangeEnd);
	}

}
