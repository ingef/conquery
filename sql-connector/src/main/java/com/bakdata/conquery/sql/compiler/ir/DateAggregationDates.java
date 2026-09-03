package com.bakdata.conquery.sql.compiler.ir;

import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jooq.Field;

/**
 * Validity-date inputs carried between query steps while the compiler prepares date aggregation.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateAggregationDates implements Qualifiable<DateAggregationDates> {

	private final List<ColumnDateRange> validityDates;

	public static DateAggregationDates forValidityDates(List<Optional<ColumnDateRange>> validityDates) {
		List<ColumnDateRange> filtered = validityDates.stream()
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
		return new DateAggregationDates(filtered);
	}

	public static DateAggregationDates forSingleStep(QueryStep queryStep) {
		List<ColumnDateRange> validityDates = queryStep.getSelects()
				.getValidityDate()
				.map(List::of)
				.orElse(Collections.emptyList());
		return new DateAggregationDates(validityDates);
	}

	public static DateAggregationDates forSteps(List<QueryStep> querySteps) {
		List<ColumnDateRange> validityDates = querySteps.stream()
				.flatMap(queryStep -> queryStep.getQualifiedSelects().getValidityDate().stream())
				.toList();
		return new DateAggregationDates(validityDates);
	}

	public boolean dateAggregationImpossible() {
		return this.validityDates.isEmpty();
	}

	public List<Field<Date>> allStarts() {
		return this.validityDates.stream().map(ColumnDateRange::getStart).toList();
	}

	public List<Field<Date>> allEnds() {
		return this.validityDates.stream().map(ColumnDateRange::getEnd).toList();
	}

	public List<SqlSelect> allStartsAndEnds() {
		return this.validityDates.stream()
				.flatMap(validityDate -> validityDate.toFields().stream())
				.<SqlSelect>map(FieldWrapper::new)
				.toList();
	}

	@Override
	public DateAggregationDates qualify(String qualifier) {
		List<ColumnDateRange> qualified = this.validityDates.stream()
				.map(validityDate -> validityDate.qualify(qualifier))
				.toList();
		return new DateAggregationDates(qualified);
	}

}
