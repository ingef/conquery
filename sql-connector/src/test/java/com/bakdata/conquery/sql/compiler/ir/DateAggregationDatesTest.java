package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import org.jooq.Field;
import org.junit.jupiter.api.Test;

class DateAggregationDatesTest {

	@Test
	void shouldCollectPresentValidityDates() {
		ColumnDateRange first = dateRange("first_start", "first_end");
		ColumnDateRange second = dateRange("second_start", "second_end");

		DateAggregationDates dates = DateAggregationDates.forValidityDates(
				List.of(Optional.empty(), Optional.of(first), Optional.of(second))
		);

		assertEquals(List.of(first, second), dates.getValidityDates());
		assertEquals(List.of("first_start", "second_start"), names(dates.allStarts()));
		assertEquals(List.of("first_end", "second_end"), names(dates.allEnds()));
		assertEquals(
				List.of("first_start", "first_end", "second_start", "second_end"),
				dates.allStartsAndEnds().stream()
						.flatMap(select -> select.toFields().stream())
						.map(Field::getName)
						.toList()
		);
		assertFalse(dates.dateAggregationImpossible());
	}

	@Test
	void shouldCollectQualifiedValidityDatesFromQuerySteps() {
		QueryStep first = queryStep("first", dateRange("valid_from", "valid_to"));
		QueryStep second = queryStep("second", dateRange("valid_from", "valid_to"));

		DateAggregationDates dates = DateAggregationDates.forSteps(List.of(first, second));

		assertEquals(
				List.of(name("first", "valid_from"), name("second", "valid_from")),
				dates.allStarts().stream().map(Field::getQualifiedName).toList()
		);
	}

	@Test
	void shouldQualifyDatesAndRecognizeEmptyInput() {
		DateAggregationDates dates = DateAggregationDates.forSingleStep(
				queryStep("source", dateRange("valid_from", "valid_to"))
		);

		DateAggregationDates qualified = dates.qualify("qualified");

		assertEquals(name("qualified", "valid_from"), qualified.allStarts().getFirst().getQualifiedName());
		assertTrue(DateAggregationDates.forValidityDates(List.of(Optional.empty())).dateAggregationImpossible());
	}

	private static QueryStep queryStep(String cteName, ColumnDateRange validityDate) {
		return QueryStep.builder()
				.cteName(cteName)
				.selects(Selects.builder()
						.ids(new SqlIdColumns(field(name("person"), String.class)))
						.validityDate(Optional.of(validityDate))
						.build())
				.build();
	}

	private static ColumnDateRange dateRange(String start, String end) {
		return ColumnDateRange.of(field(name(start), Date.class), field(name(end), Date.class));
	}

	private static List<String> names(List<? extends Field<?>> fields) {
		return fields.stream().map(Field::getName).toList();
	}
}
