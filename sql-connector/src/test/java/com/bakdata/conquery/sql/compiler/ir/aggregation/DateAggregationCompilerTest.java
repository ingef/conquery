package com.bakdata.conquery.sql.compiler.ir.aggregation;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.DateAggregationDates;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import org.jooq.Field;
import org.junit.jupiter.api.Test;

class DateAggregationCompilerTest {

	private static final CompilerDialect DIALECT = new TestDialect();

	@Test
	void shouldMergeDatesAndPackTheResultingIntervals() {
		QueryStep result = DateAggregationCompiler.aggregate(
				joinedStep(),
				List.of(),
				dateInputs(),
				DateAggregationAction.MERGE,
				DIALECT,
				new SqlNameGenerator(128)
		);

		assertEquals("joined-merge-interval_complete", result.getCteName());
		assertTrue(result.getSelects().getValidityDate().isPresent());
		assertEquals("joined-merge-range_index", result.getPredecessors().getFirst().getCteName());
	}

	@Test
	void shouldIntersectDatesWithoutAdditionalIntervalPacking() {
		QueryStep result = DateAggregationCompiler.aggregate(
				joinedStep(),
				List.of(),
				dateInputs(),
				DateAggregationAction.INTERSECT,
				DIALECT,
				new SqlNameGenerator(128)
		);

		assertEquals("joined-merge", result.getCteName());
		assertTrue(result.isUnion());
	}

	@Test
	void shouldInvertAggregatedIntervals() {
		QueryStep base = stepWithValidityDate("base", dateRange("valid_from", "valid_to"));

		QueryStep result = DateAggregationCompiler.invert(base, DIALECT, new SqlNameGenerator(128));

		assertEquals("base-inverted_dates", result.getCteName());
		assertEquals("base-row_numbers", result.getPredecessors().getFirst().getCteName());
		assertTrue(result.getSelects().getValidityDate().isPresent());
	}

	@Test
	void shouldReturnOriginalStepWhenThereAreNoDatesToInvert() {
		QueryStep base = joinedStep();

		assertSame(base, DateAggregationCompiler.invert(base, DIALECT, new SqlNameGenerator(128)));
	}

	@Test
	void shouldRejectNonAggregationActions() {
		assertThrows(
				IllegalStateException.class,
				() -> DateAggregationCompiler.aggregate(
						joinedStep(),
						List.of(),
						dateInputs(),
						DateAggregationAction.BLOCK,
						DIALECT,
						new SqlNameGenerator(128)
				)
		);
	}

	private static QueryStep joinedStep() {
		return QueryStep.builder()
				.cteName("joined")
				.selects(Selects.builder()
						.ids(new SqlIdColumns(field(name("person"), String.class)))
						.build())
				.build();
	}

	private static QueryStep stepWithValidityDate(String cteName, ColumnDateRange dateRange) {
		return QueryStep.builder()
				.cteName(cteName)
				.selects(Selects.builder()
						.ids(new SqlIdColumns(field(name("person"), String.class)))
						.validityDate(Optional.of(dateRange))
						.build())
				.build();
	}

	private static DateAggregationDates dateInputs() {
		return DateAggregationDates.forValidityDates(List.of(
				Optional.of(dateRange("first_start", "first_end")),
				Optional.of(dateRange("second_start", "second_end"))
		));
	}

	private static ColumnDateRange dateRange(String start, String end) {
		return ColumnDateRange.of(field(name(start), Date.class), field(name(end), Date.class));
	}

	private static final class TestDialect implements CompilerDialect {

		@Override
		public Field<Date> minimumDate() {
			return field(name("minimum_date"), Date.class);
		}

		@Override
		public Field<Date> maximumDate() {
			return field(name("maximum_date"), Date.class);
		}

		@Override
		public <T> Field<T> anyValue(Field<T> value) {
			return value;
		}

		@Override
		public Field<?> renderDateRange(Field<Date> start, Field<Date> end) {
			return field(name("rendered_range"), Object.class);
		}

		@Override
		public Field<?> aggregateDateRanges(Field<Date> start, Field<Date> end) {
			return field(name("aggregated_ranges"), Object.class);
		}

		@Override
		public int getNameMaxLength() {
			return 128;
		}
	}
}
