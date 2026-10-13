package com.bakdata.conquery.sql.compiler.ir.interval;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import org.jooq.Field;
import org.junit.jupiter.api.Test;

class IntervalPackingTest {

	private static final SqlIdColumns IDS = new SqlIdColumns(field(name("person"), String.class));
	private static final ColumnDateRange DATE_RANGE = ColumnDateRange.of(
			field(name("valid_from"), Date.class),
			field(name("valid_to"), Date.class)
	);

	@Test
	void shouldCreateThreeStepGraphForSeparateRangeColumns() {
		SqlTables tables = IntervalPackingCteStep.createTables(
				sourceStep(),
				new TestDialect(false),
				new SqlNameGenerator(128)
		);

		assertEquals("events-previous_end", tables.cteName(IntervalPackingCteStep.PREVIOUS_END));
		assertEquals("events-range_index", tables.cteName(IntervalPackingCteStep.RANGE_INDEX));
		assertEquals("events-interval_complete", tables.cteName(IntervalPackingCteStep.INTERVAL_COMPLETE));
		assertEquals("events", tables.getPredecessor(IntervalPackingCteStep.PREVIOUS_END));
		assertEquals("events-previous_end", tables.getPredecessor(IntervalPackingCteStep.RANGE_INDEX));
		assertEquals("events-range_index", tables.getPredecessor(IntervalPackingCteStep.INTERVAL_COMPLETE));
	}

	@Test
	void shouldSkipIntermediateStepsForSingleColumnRanges() {
		SqlTables tables = IntervalPackingCteStep.createTables(
				sourceStep(),
				new TestDialect(true),
				new SqlNameGenerator(128)
		);

		assertFalse(tables.isRequiredStep(IntervalPackingCteStep.PREVIOUS_END));
		assertFalse(tables.isRequiredStep(IntervalPackingCteStep.RANGE_INDEX));
		assertTrue(tables.isRequiredStep(IntervalPackingCteStep.INTERVAL_COMPLETE));
		assertEquals("events", tables.getPredecessor(IntervalPackingCteStep.INTERVAL_COMPLETE));
	}

	@Test
	void shouldBuildAnsiPackingStepsOverConnectorIr() {
		QueryStep source = sourceStep();
		SqlTables tables = IntervalPackingCteStep.createTables(
				source,
				new TestDialect(false),
				new SqlNameGenerator(128)
		);
		IntervalPackingContext context = IntervalPackingContext.builder()
				.ids(IDS)
				.daterange(DATE_RANGE)
				.predecessor(Optional.of(source))
				.tables(tables)
				.build();

		QueryStep intervalComplete = AnsiSqlIntervalPacker.aggregateAsValidityDate(context);
		QueryStep rangeIndex = intervalComplete.getPredecessors().getFirst();
		QueryStep previousEnd = rangeIndex.getPredecessors().getFirst();

		assertEquals("events-interval_complete", intervalComplete.getCteName());
		assertEquals("events-range_index", rangeIndex.getCteName());
		assertEquals("events-previous_end", previousEnd.getCteName());
		assertEquals(List.of(source), previousEnd.getPredecessors());
		assertTrue(intervalComplete.getSelects().getValidityDate().isPresent());
		assertEquals(DATE_RANGE.getStart().getName(), intervalComplete.getSelects().getValidityDate().orElseThrow().getStart().getName());
	}

	private static QueryStep sourceStep() {
		return QueryStep.builder()
				.cteName("events")
				.selects(Selects.builder()
						.ids(IDS)
						.validityDate(Optional.of(DATE_RANGE))
						.build())
				.build();
	}

	private record TestDialect(boolean supportsSingleColumnRanges) implements CompilerDialect {

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
