package com.bakdata.conquery.sql.compiler.conversion.operation;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.model.schema.DateColumns;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class ResolvedAggregationConverterTest {

	private static final SqlTable TABLE = SqlTable.of("events", "analytics", "events");
	private static final ResolvedColumn VALUE = column("value", ColumnType.STRING);
	private static final ResolvedColumn PERSON = column("person_id", ColumnType.STRING);
	private static final ResolvedColumn AMOUNT = column("amount", ColumnType.DECIMAL);
	private static final ResolvedColumn DISCOUNT = column("discount", ColumnType.DECIMAL);
	private static final ResolvedColumn START = column("start_date", ColumnType.DATE);
	private static final ResolvedColumn END = column("end_date", ColumnType.DATE);
	private static final SqlTables TABLES = new SqlTables(
			"events",
			Map.of(ConceptCteStep.PREPROCESSING, "preprocessing"),
			Map.of(ConceptCteStep.AGGREGATION_SELECT, ConceptCteStep.PREPROCESSING)
	);
	private static final AggregationConversionContext CONTEXT = new AggregationConversionContext(
			new TestDialect(),
			new SqlNameGenerator(128),
			TABLES,
			new SqlIdColumns(field(name("events", "id"), String.class)),
			"event-count"
	);

	private final ResolvedAggregationConverter converter = new ResolvedAggregationConverter();

	@Test
	void shouldConvertCount() {
		CommonAggregationSelect<?> result = converter.convert(
				new BuiltInAggregations.Count(VALUE, List.of()),
				CONTEXT
		);

		assertEquals(List.of("\"events\".\"value\""), renderRootSelects(result));
		assertEquals(
				"nullif(count(\"preprocessing\".\"value\"), 0) as \"event-count\"",
				render(result.getGroupBy().select())
		);
	}

	@Test
	void shouldCountDistinctValues() {
		CommonAggregationSelect<?> result = converter.convert(
				new BuiltInAggregations.Count(VALUE, List.of(PERSON)),
				CONTEXT
		);

		assertEquals(List.of("\"events\".\"value\"", "\"events\".\"person_id\""), renderRootSelects(result));
		assertEquals(
				"nullif(count(distinct \"preprocessing\".\"person_id\"), 0) as \"event-count\"",
				render(result.getGroupBy().select())
		);
	}

	private static List<String> renderRootSelects(CommonAggregationSelect<?> aggregation) {
		return aggregation.getRootSelects().stream()
				.map(select -> render(select.select()))
				.toList();
	}

	@Test
	void shouldConvertSumWithoutReplacingMissingValuesWithZero() {
		CommonAggregationSelect<?> result = converter.convert(new BuiltInAggregations.Sum(AMOUNT, Optional.empty(), List.of()), CONTEXT);

		assertEquals(List.of("\"events\".\"amount\""), renderRootSelects(result));
		assertEquals("sum(\"preprocessing\".\"amount\") as \"event-count\"", render(result.getGroupBy().select()));
		assertTrue(result.getAdditionalPredecessor().isEmpty());
	}

	@Test
	void shouldSubtractWithZeroOnlyWhenAnOperandIsPresent() {
		CommonAggregationSelect<?> result = converter.convert(new BuiltInAggregations.Sum(AMOUNT, Optional.of(DISCOUNT), List.of()), CONTEXT);

		assertEquals(List.of("\"events\".\"amount\"", "\"events\".\"discount\""), renderRootSelects(result));
		String zero = "coalesce((\"preprocessing\".\"amount\" * 0), (\"preprocessing\".\"discount\" * 0))";
		assertEquals(
				"sum((coalesce(\"preprocessing\".\"amount\", " + zero + ") - coalesce(\"preprocessing\".\"discount\", " + zero + "))) as \"event-count\"",
				render(result.getGroupBy().select())
		);
	}

	@Test
	void shouldSumDistinctRowsInSeparateBranchPartitionedByAllIdsAndKeys() {
		AggregationConversionContext context = new AggregationConversionContext(
				new TestDialect(), new SqlNameGenerator(128), TABLES,
				new SqlIdColumns(field(name("events", "id"), String.class), field(name("events", "secondary_id"), String.class)), "total"
		);
		CommonAggregationSelect<?> result = converter.convert(new BuiltInAggregations.Sum(AMOUNT, Optional.empty(), List.of(PERSON, AMOUNT)), context);
		QueryStep summed = result.getAdditionalPredecessor().orElseThrow();
		QueryStep numbered = summed.getPredecessors().getFirst();

		assertEquals(List.of("\"events\".\"amount\"", "\"events\".\"person_id\""), renderRootSelects(result));
		assertEquals(
				"row_number() over (partition by \"preprocessing\".\"id\", \"preprocessing\".\"secondary_id\", \"preprocessing\".\"person_id\", \"preprocessing\".\"amount\") as \"row_number\"",
				render(numbered.getSelects().getSqlSelects().getLast().toFields().getFirst())
		);
		assertEquals("\"total-row_number_assigned\".\"row_number\" = 1", DSL.using(SQLDialect.POSTGRES).renderInlined(summed.getConditions().getFirst()));
		assertEquals(2, summed.getGroupBy().size());
		assertEquals("sum(coalesce(\"total-row_number_assigned\".\"amount\", 0)) as \"total\"", render(result.getGroupBy().select()));
		assertEquals(List.of(result.getGroupBy()), summed.getSelects().getSqlSelects());
	}

	@Test
	void shouldIgnoreSubtractionInsideDistinctSumLikeExistingBackend() {
		CommonAggregationSelect<?> result = converter.convert(new BuiltInAggregations.Sum(AMOUNT, Optional.of(DISCOUNT), List.of(PERSON)), CONTEXT);
		QueryStep numbered = result.getAdditionalPredecessor().orElseThrow().getPredecessors().getFirst();

		assertEquals(2, numbered.getSelects().getSqlSelects().size());
		assertEquals("sum(coalesce(\"event-count-row_number_assigned\".\"amount\", 0)) as \"event-count\"", render(result.getGroupBy().select()));
		assertEquals(List.of("\"events\".\"amount\"", "\"events\".\"person_id\""), renderRootSelects(result));
	}

	@Test
	void shouldPreserveNumericInputTypes() {
		Map<ColumnType, Class<?>> types = Map.of(
				ColumnType.INTEGER, Integer.class,
				ColumnType.REAL, Double.class,
				ColumnType.DECIMAL, Double.class,
				ColumnType.MONEY, java.math.BigDecimal.class
		);
		for (var entry : types.entrySet()) {
			ResolvedColumn amount = column("amount", entry.getKey());
			ResolvedColumn discount = column("discount", entry.getKey());
			for (List<ResolvedColumn> distinctBy : List.of(List.<ResolvedColumn>of(), List.of(PERSON))) {
				CommonAggregationSelect<?> result = converter.convert(
						new BuiltInAggregations.Sum(amount, Optional.of(discount), distinctBy), CONTEXT);
				assertEquals(entry.getValue(), result.getRootSelects().getFirst().select().getType(), entry.getKey().name());
			}
		}
	}

	@Test
	void shouldCountDistinctYearQuartersForSingleDates() {
		CommonAggregationSelect<?> result = converter.convert(new BuiltInAggregations.CountQuarters(new DateColumns.Single(START)), CONTEXT);

		assertEquals(List.of("\"events\".\"start_date\""), renderRootSelects(result));
		assertEquals("nullif(count(distinct year_quarter(\"preprocessing\".\"start_date\")), 0) as \"event-count\"", render(result.getGroupBy().select()));
	}

	@Test
	void shouldSumQuarterCountsForDatePairs() {
		CommonAggregationSelect<?> result = converter.convert(new BuiltInAggregations.CountQuarters(new DateColumns.Pair(START, END)), CONTEXT);

		assertEquals(List.of("(date_distance('months', quarter_start(\"events\".\"start_date\"), next_quarter_start(\"events\".\"end_date\")) / 3) as \"event-count\""), renderRootSelects(result));
		assertEquals("nullif(sum(\"preprocessing\".\"event-count\"), 0) as \"event-count\"", render(result.getGroupBy().select()));
	}

	@Test
	void shouldPackIntervalsBeforeSummingDurationsAndExcludeInfiniteBounds() {
		CommonAggregationSelect<?> result = converter.convert(new BuiltInAggregations.DurationSum(new DateColumns.Pair(START, END), List.of()), CONTEXT);
		QueryStep sum = result.getAdditionalPredecessor().orElseThrow();

		assertEquals(List.of(
				"coalesce(\"events\".\"start_date\", \"minimum_date\") as \"event-count_start\"",
				"coalesce(add_days(\"events\".\"end_date\", 1), \"maximum_date\") as \"event-count_end\""
		), renderRootSelects(result));
		assertEquals("event-count-interval_packing_selects", sum.getCteName());
		assertEquals("event-count-interval_complete", sum.getPredecessors().getFirst().getCteName());
		assertEquals(List.of(result.getGroupBy()), sum.getSelects().getSqlSelects());
		String sql = render(result.getGroupBy().select());
		assertTrue(sql.contains("\"event-count-interval_complete\".\"event-count_start\" = \"minimum_date\""), sql);
		assertTrue(sql.contains("\"event-count-interval_complete\".\"event-count_end\" = \"maximum_date\""), sql);
		assertTrue(sql.contains("then null else date_distance('days'"), sql);
	}

	@Test
	void shouldTreatSingleDateAsOneInclusiveDayForDuration() {
		CommonAggregationSelect<?> result = converter.convert(new BuiltInAggregations.DurationSum(new DateColumns.Single(START), List.of()), CONTEXT);
		assertEquals("coalesce(add_days(\"events\".\"start_date\", 1), \"maximum_date\") as \"event-count_end\"", renderRootSelects(result).getLast());
	}

	@Test
	void shouldIgnoreDurationDistinctKeysLikeExistingBackend() {
		CommonAggregationSelect<?> result = converter.convert(
				new BuiltInAggregations.DurationSum(new DateColumns.Pair(START, END), List.of(PERSON)), CONTEXT);
		assertEquals(List.of(
				"coalesce(\"events\".\"start_date\", \"minimum_date\") as \"event-count_start\"",
				"coalesce(add_days(\"events\".\"end_date\", 1), \"maximum_date\") as \"event-count_end\""
		), renderRootSelects(result));
		assertEquals("event-count-interval_packing_selects", result.getAdditionalPredecessor().orElseThrow().getCteName());
	}

	@Test
	void shouldRequireDialectSupportForPhysicalRangeColumns() {
		assertThrows(UnsupportedOperationException.class, () -> converter.convert(
				new BuiltInAggregations.DurationSum(new DateColumns.Single(column("range", ColumnType.DATE_RANGE)), List.of()), CONTEXT
		));
	}

	private static String render(Field<?> field) {
		return DSL.using(SQLDialect.POSTGRES).renderInlined(DSL.select(field)).substring("select ".length()).toLowerCase(Locale.ROOT);
	}

	private static ResolvedColumn column(String name, ColumnType type) {
		return new ResolvedColumn("events." + name, TABLE, name, type, true);
	}

	private static final class TestDialect implements CompilerDialect {

		@Override
		public Field<Integer> dateDistance(ChronoUnit unit, Field<Date> start, Field<Date> end) {
			return DSL.function("date_distance", Integer.class, DSL.inline(unit.name()), start, end);
		}

		@Override
		public Field<Date> addDays(Field<Date> date, Field<Integer> days) {
			return DSL.function("add_days", Date.class, date, days);
		}

		@Override
		public Field<String> yearQuarter(Field<Date> date) {
			return DSL.function("year_quarter", String.class, date);
		}

		@Override
		public Field<Date> quarterStart(Field<Date> date) {
			return DSL.function("quarter_start", Date.class, date);
		}

		@Override
		public Field<Date> nextQuarterStart(Field<Date> date) {
			return DSL.function("next_quarter_start", Date.class, date);
		}

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
