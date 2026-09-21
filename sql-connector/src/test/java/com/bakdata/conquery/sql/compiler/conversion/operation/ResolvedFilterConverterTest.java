package com.bakdata.conquery.sql.compiler.conversion.operation;

import static org.jooq.impl.DSL.condition;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.compiler.conversion.Converter;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.condition.ConditionWrappingWhereCondition;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.concept.SqlFilters;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.model.operation.BuiltInFilters;
import com.bakdata.conquery.sql.model.operation.ResolvedFilter;
import com.bakdata.conquery.sql.model.operation.ResolvedAggregation;
import com.bakdata.conquery.sql.model.range.NumberRange;
import com.bakdata.conquery.sql.model.range.SubstringRange;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.DateColumns;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class ResolvedFilterConverterTest {

	private static final SqlTable TABLE = SqlTable.of("events", "analytics", "events");
	private static final ResolvedColumn STRING_COLUMN = new ResolvedColumn(
			"code", TABLE, "event_code", ColumnType.STRING, true
	);
	private static final ResolvedColumn NUMBER_COLUMN = new ResolvedColumn(
			"score", TABLE, "score", ColumnType.DECIMAL, true
	);
	private static final ResolvedColumn DATE_COLUMN = new ResolvedColumn(
			"start", TABLE, "start_date", ColumnType.DATE, true
	);
	private static final ResolvedColumn FLAG_A = new ResolvedColumn(
			"flag-a", TABLE, "flag_a", ColumnType.BOOLEAN, false
	);
	private static final ResolvedColumn FLAG_B = new ResolvedColumn(
			"flag-b", TABLE, "flag_b", ColumnType.BOOLEAN, false
	);
	private final FilterConversionContext CONTEXT = new FilterConversionContext(
			new TestDialect(),
			new SqlNameGenerator(128),
			new SqlTables(
					"events",
					Map.of(
							ConceptCteStep.PREPROCESSING, "preprocessing",
							ConceptCteStep.JOIN_BRANCHES, "join_branches"
					),
					Map.of(
							ConceptCteStep.AGGREGATION_SELECT, ConceptCteStep.PREPROCESSING,
							ConceptCteStep.AGGREGATION_FILTER, ConceptCteStep.JOIN_BRANCHES
					)
			),
			new SqlIdColumns(field(name("analytics", "events", "id"), String.class))
	);

	private final ResolvedFilterConverter converter = new ResolvedFilterConverter();

	@Test
	void shouldConvertStringValuesWithSubstring() {
		BuiltInFilters.StringValues filter = new BuiltInFilters.StringValues(
				"codes",
				STRING_COLUMN,
				new LinkedHashSet<>(List.of("AA", "BB")),
				Optional.of(SubstringRange.between(1, 3))
		);

		SqlFilters result = converter.convert(filter, CONTEXT);

		assertEquals(
				"substring(\"analytics\".\"events\".\"event_code\", 2, 2) in ('aa', 'bb')",
				render(result)
		);
		assertEmptySelects(result.getSelects());
	}

	@Test
	void shouldConvertNumericColumnRange() {
		BuiltInFilters.NumericColumnRange filter = new BuiltInFilters.NumericColumnRange(
				"score",
				NUMBER_COLUMN,
				NumberRange.atLeast(10.5)
		);

		SqlFilters result = converter.convert(filter, CONTEXT);

		assertEquals("\"analytics\".\"events\".\"score\" >= 10.5", render(result));
		assertEmptySelects(result.getSelects());
	}

	@Test
	void shouldConvertDateDistanceRangeThroughDialect() {
		BuiltInFilters.DateDistanceRange filter = new BuiltInFilters.DateDistanceRange(
				"age",
				DATE_COLUMN,
				ChronoUnit.YEARS,
				LocalDate.of(2025, 2, 1),
				NumberRange.closed(18, 65)
		);

		SqlFilters result = converter.convert(filter, CONTEXT);

		assertEquals(
				"(date_distance('years', \"analytics\".\"events\".\"start_date\", date '2025-02-01') >= 18 and date_distance('years', \"analytics\".\"events\".\"start_date\", date '2025-02-01') <= 65)",
				render(result)
		);
	}

	@Test
	void shouldConvertSelectedFlags() {
		BuiltInFilters.Flags filter = new BuiltInFilters.Flags(
				"flags",
				Map.of("A", FLAG_A, "B", FLAG_B),
				Set.of("A", "B")
		);

		SqlFilters result = converter.convert(filter, CONTEXT);

		assertTrue(
				Set.of(
						"(\"analytics\".\"events\".\"flag_a\" = true or \"analytics\".\"events\".\"flag_b\" = true)",
						"(\"analytics\".\"events\".\"flag_b\" = true or \"analytics\".\"events\".\"flag_a\" = true)"
				).contains(render(result))
		);
	}

	@Test
	void shouldConvertAggregationRange() {
		BuiltInFilters.AggregationRange filter = new BuiltInFilters.AggregationRange(
				"event count",
				new BuiltInAggregations.Count(STRING_COLUMN, List.of()),
				NumberRange.atLeast(2)
		);

		SqlFilters result = converter.convert(filter, CONTEXT);

		assertEquals("\"join_branches\".\"event_count-1\" >= 2", renderGroupFilter(result));
		assertEquals(
				"nullif(count(\"preprocessing\".\"event_code\"), 0) as \"event_count-1\"",
				renderField(result.getSelects().getAggregationSelects().getFirst().toFields().getFirst())
		);
		assertEquals(
				"\"events\".\"event_code\"",
				renderField(result.getSelects().getPreprocessingSelects().getFirst().toFields().getFirst())
		);
	}

	@Test
	void shouldDispatchExtensionFilter() {
		Converter<ExtensionFilter, SqlFilters, FilterConversionContext> extension = new Converter<>() {
			@Override
			public Class<ExtensionFilter> getConversionClass() {
				return ExtensionFilter.class;
			}

			@Override
			public SqlFilters convert(ExtensionFilter input, FilterConversionContext context) {
				return new SqlFilters(
						ConnectorSqlSelects.none(),
						WhereClauses.builder()
								.eventFilter(new ConditionWrappingWhereCondition(condition("extension_filter")))
								.build()
				);
			}
		};
		ResolvedFilterConverter converterWithExtension = new ResolvedFilterConverter(List.of(extension));

		assertEquals("(extension_filter)", render(converterWithExtension.convert(new ExtensionFilter("extension"), CONTEXT)));
	}

	@Test
	void shouldFilterDistinctSumFromJoinedBranchWithoutRepeatingAggregation() {
		SqlFilters result = converter.convert(new BuiltInFilters.AggregationRange(
				"distinct total", new BuiltInAggregations.Sum(NUMBER_COLUMN, Optional.empty(), List.of(STRING_COLUMN)), NumberRange.closed(2, 8)
		), CONTEXT);

		assertTrue(result.getSelects().getAggregationSelects().isEmpty());
		assertEquals("distinct_total-1-row_number_filtered", result.getSelects().getAdditionalPredecessor().orElseThrow().getCteName());
		assertEquals("(\"join_branches\".\"distinct_total-1\" >= 2 and \"join_branches\".\"distinct_total-1\" <= 8)", renderGroupFilter(result));
	}

	@Test
	void shouldFilterDurationFromIntervalPackingBranch() {
		SqlFilters result = converter.convert(new BuiltInFilters.AggregationRange(
				"duration", new BuiltInAggregations.DurationSum(new DateColumns.Single(DATE_COLUMN), List.of()), NumberRange.atMost(10)
		), CONTEXT);

		assertTrue(result.getSelects().getAggregationSelects().isEmpty());
		assertEquals("duration-1-interval_packing_selects", result.getSelects().getAdditionalPredecessor().orElseThrow().getCteName());
		assertEquals("\"join_branches\".\"duration-1\" <= 10", renderGroupFilter(result));
	}

	@Test
	void shouldDispatchCustomAggregationInsideRangeFilter() {
		FieldWrapper<Integer> value = new FieldWrapper<>(DSL.inline(42).as("custom-1"));
		QueryStep predecessor = QueryStep.builder().cteName("custom_result")
				.selects(Selects.builder().ids(CONTEXT.ids()).sqlSelect(value).build()).build();
		ResolvedAggregationConverter custom = new ResolvedAggregationConverter(List.of(
				new Converter<ExtensionAggregation, CommonAggregationSelect<?>, AggregationConversionContext>() {
					@Override
					public Class<ExtensionAggregation> getConversionClass() {
						return ExtensionAggregation.class;
					}

					@Override
					public CommonAggregationSelect<?> convert(ExtensionAggregation input, AggregationConversionContext context) {
						assertEquals("custom-1", context.alias());
						assertSame(CONTEXT.ids(), context.ids());
						return CommonAggregationSelect.<Integer>builder().groupBy(value).additionalPredecessor(predecessor).build();
					}
				}
		));
		SqlFilters result = new ResolvedFilterConverter(List.of(), custom).convert(
				new BuiltInFilters.AggregationRange("custom", new ExtensionAggregation(), NumberRange.atLeast(1)), CONTEXT
		);

		assertSame(predecessor, result.getSelects().getAdditionalPredecessor().orElseThrow());
		assertTrue(result.getSelects().getAggregationSelects().isEmpty());
		assertEquals("\"join_branches\".\"custom-1\" >= 1", renderGroupFilter(result));
	}

	@Test
	void shouldRejectNonnumericAggregationInRangeFilter() {
		ResolvedAggregationConverter custom = new ResolvedAggregationConverter(List.of(
				new Converter<ExtensionAggregation, CommonAggregationSelect<?>, AggregationConversionContext>() {
					@Override
					public Class<ExtensionAggregation> getConversionClass() {
						return ExtensionAggregation.class;
					}

					@Override
					public CommonAggregationSelect<?> convert(ExtensionAggregation input, AggregationConversionContext context) {
						return CommonAggregationSelect.<String>builder().groupBy(new FieldWrapper<>(DSL.inline("flag").as(context.alias()))).build();
					}
				}
		));
		assertThrows(IllegalArgumentException.class, () -> new ResolvedFilterConverter(List.of(), custom).convert(
				new BuiltInFilters.AggregationRange("invalid", new ExtensionAggregation(), NumberRange.atLeast(1)), CONTEXT
		));
	}

	private record ExtensionAggregation() implements ResolvedAggregation {
	}

	private static String render(SqlFilters filters) {
		assertEquals(1, filters.getWhereClauses().getEventFilters().size());
		assertTrue(filters.getWhereClauses().getPreprocessingConditions().isEmpty());
		assertTrue(filters.getWhereClauses().getGroupFilters().isEmpty());
		return DSL.using(SQLDialect.POSTGRES)
				.renderInlined(filters.getWhereClauses().getEventFilters().getFirst().condition())
				.toLowerCase(Locale.ROOT);
	}

	private static String renderGroupFilter(SqlFilters filters) {
		assertEquals(1, filters.getWhereClauses().getGroupFilters().size());
		assertTrue(filters.getWhereClauses().getPreprocessingConditions().isEmpty());
		assertTrue(filters.getWhereClauses().getEventFilters().isEmpty());
		return DSL.using(SQLDialect.POSTGRES)
				.renderInlined(filters.getWhereClauses().getGroupFilters().getFirst().condition())
				.toLowerCase(Locale.ROOT);
	}

	private static String renderField(Field<?> field) {
		return DSL.using(SQLDialect.POSTGRES).renderInlined(DSL.select(field)).substring("select ".length()).toLowerCase(Locale.ROOT);
	}

	private static void assertEmptySelects(ConnectorSqlSelects selects) {
		assertTrue(selects.getPreprocessingSelects().isEmpty());
		assertTrue(selects.getAggregationSelects().isEmpty());
		assertTrue(selects.getEventDateSelects().isEmpty());
		assertTrue(selects.getFinalSelects().isEmpty());
		assertTrue(selects.getAdditionalPredecessor().isEmpty());
	}

	private record ExtensionFilter(String name) implements ResolvedFilter {
	}

	private static final class TestDialect implements CompilerDialect {

		@Override
		public Field<Integer> dateDistance(ChronoUnit unit, Field<Date> start, Field<Date> end) {
			return DSL.function("date_distance", Integer.class, DSL.inline(unit.name()), start, end);
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
		public Field<Integer> dateDistance(ChronoUnit unit, Field<Date> startDate, LocalDate endDate) {
			return DSL.function(
					"date_distance",
					Integer.class,
					DSL.inline(unit.name()),
					startDate,
					DSL.inline(Date.valueOf(endDate))
			);
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
