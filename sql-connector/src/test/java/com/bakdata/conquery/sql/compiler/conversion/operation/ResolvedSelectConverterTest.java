package com.bakdata.conquery.sql.compiler.conversion.operation;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.model.operation.ResolvedSelect;
import com.bakdata.conquery.sql.model.range.SubstringRange;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import com.bakdata.conquery.sql.model.schema.DateColumns;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class ResolvedSelectConverterTest {

	private static final ResolvedColumn VALUE = new ResolvedColumn("events.value", SqlTable.of("events", "events"), "value", ColumnType.STRING, true);
	private final ResolvedSelectConverter converter = new ResolvedSelectConverter();

	@Test
	void shouldRejectMissingSelectRegistration() {
		assertThrows(IllegalStateException.class, () -> converter.convert(new UnsupportedSelect("value"), context()));
	}

	@Test
	void shouldProjectExistsForConnectorAndConceptConversions() {
		var connector = converter.convert(new BuiltInSelects.Exists("exists"), context());
		var concept = converter.conceptSelect(new BuiltInSelects.Exists("exists"), context());

		assertEquals("1 as \"value\"", render(connector.getFinalSelects().getFirst().toFields().getFirst()));
		assertEquals("1 as \"value\"", render(concept.getFinalSelects().getFirst().toFields().getFirst()));
		assertEquals(List.of(), connector.getFinalSelects().getFirst().requiredColumns());
		assertEquals(1, connector.getFinalSelects().size());
		assertEquals(1, concept.getFinalSelects().size());
		assertTrue(connector.getPreprocessingSelects().isEmpty());
		assertTrue(connector.getAdditionalPredecessor().isEmpty());
	}

	@Test
	void shouldSelfUnionOneConceptValueColumn() {
		var result = converter.conceptSelect(new BuiltInSelects.ConceptValues("value", List.of(VALUE)), context());
		var union = result.getAdditionalPredecessor().orElseThrow().getPredecessors().getFirst();
		assertEquals(1, union.getUnion().size());
		assertFalse(union.isUnionAll());
	}

	@Test
	void shouldUsePreparedConceptIdExpressionForConnectorPreprocessing() {
		var source = new SelectConversionContext.ConceptColumnSource(
				"events",
				DSL.table(DSL.name("events")),
				DSL.coalesce(DSL.field(DSL.name("concept_ids", "resolved_id"), Integer.class), DSL.inline(0)),
				List.of()
		);
		var result = converter.convert(
				new BuiltInSelects.ConceptValues("value", List.of(VALUE)),
				context(List.of(source))
		);

		assertEquals("coalesce(\"concept_ids\".\"resolved_id\", 0) as \"value\"",
				render(result.getPreprocessingSelects().getFirst().toFields().getFirst()));
	}

	@Test
	void shouldUsePreparedConceptIdJoinForUnconvertedConceptConnector() {
		var events = DSL.table(DSL.name("events"));
		var mapping = DSL.table(DSL.name("concept_ids"));
		var sourceTable = events.leftJoin(mapping).on(
				DSL.field(DSL.name("events", "value")).eq(DSL.field(DSL.name("concept_ids", "value")))
		);
		var rawValuePresent = DSL.field(DSL.name("events", "value")).isNotNull();
		var source = new SelectConversionContext.ConceptColumnSource(
				"events",
				sourceTable,
				DSL.coalesce(DSL.field(DSL.name("concept_ids", "resolved_id"), Integer.class), DSL.inline(0)),
				List.of(rawValuePresent)
		);
		var result = converter.conceptSelect(
				new BuiltInSelects.ConceptValues("value", List.of(VALUE)),
				context(List.of(source))
		);
		var union = result.getAdditionalPredecessor().orElseThrow().getPredecessors().getFirst();

		assertTrue(DSL.using(SQLDialect.POSTGRES).renderInlined(union.getFromTables().getFirst()).toLowerCase(Locale.ROOT)
				.contains("left outer join \"concept_ids\""));
		assertEquals(List.of(rawValuePresent), union.getConditions());
		assertEquals("cast(coalesce(\"concept_ids\".\"resolved_id\", 0) as varchar) as \"value\"",
				render(union.getSelects().getSqlSelects().getFirst().toFields().getFirst()));
	}

	@Test
	void shouldKeepPreparedSourcesDistinctForDuplicateConceptColumns() {
		var firstSource = new SelectConversionContext.ConceptColumnSource(
				"events",
				DSL.table(DSL.name("events")).leftJoin(DSL.table(DSL.name("concept_ids_a"))).on(DSL.trueCondition()),
				DSL.field(DSL.name("concept_ids_a", "resolved_id"), Integer.class),
				List.of()
		);
		var secondSource = new SelectConversionContext.ConceptColumnSource(
				"events",
				DSL.table(DSL.name("events")).leftJoin(DSL.table(DSL.name("concept_ids_b"))).on(DSL.trueCondition()),
				DSL.field(DSL.name("concept_ids_b", "resolved_id"), Integer.class),
				List.of()
		);
		var result = converter.conceptSelect(
				new BuiltInSelects.ConceptValues("value", List.of(VALUE, VALUE)),
				context(List.of(firstSource, secondSource))
		);
		var union = result.getAdditionalPredecessor().orElseThrow().getPredecessors().getFirst();

		assertTrue(DSL.using(SQLDialect.POSTGRES).renderInlined(union.getFromTables().getFirst()).contains("concept_ids_a"));
		assertTrue(DSL.using(SQLDialect.POSTGRES).renderInlined(union.getUnion().getFirst().getFromTables().getFirst()).contains("concept_ids_b"));
	}

	@Test
	void shouldDispatchDateUnionToIntervalPacking() {
		var date = new ResolvedColumn("events.date", VALUE.table(), "date", ColumnType.DATE, true);
		var result = converter.convert(new BuiltInSelects.DateUnion("value", new DateColumns.Single(date)), context());
		assertEquals("value-interval_packing_selects", result.getAdditionalPredecessor().orElseThrow().getCteName());
		assertEquals(2, result.getPreprocessingSelects().size());
		assertTrue(result.getAggregationSelects().isEmpty());
	}

	@Test
	void shouldKeepEventDatesInEventBranch() {
		var base = context();
		var dates = ColumnDateRange.of(DSL.field(DSL.name("start"), Date.class), DSL.field(DSL.name("end"), Date.class));
		var context = new SelectConversionContext(base.dialect(), base.nameGenerator(), base.tables(), base.ids(), Optional.of(dates), base.alias());
		for (var operation : List.of(new BuiltInSelects.EventDateUnion("value"), new BuiltInSelects.EventDurationSum("value"))) {
			var result = converter.convert(operation, context);
			assertEquals(1, result.getEventDateSelects().size());
			assertTrue(result.getAggregationSelects().isEmpty());
		}
	}

	@Test
	void shouldCompileFlagsThroughAggregation() {
		var flag = new ResolvedColumn("events.flag", VALUE.table(), "flag", ColumnType.BOOLEAN, true);
		var result = converter.convert(new BuiltInSelects.Aggregation("value", new BuiltInAggregations.Flags(Map.of("A", flag))), context());
		assertEquals(1, result.getAggregationSelects().size());
		assertTrue(render(result.getAggregationSelects().getFirst().toFields().getFirst()).contains("then 'a'"));
	}

	@Test
	void shouldCompileDateDistanceMinimum() {
		var date = new ResolvedColumn("events.date", VALUE.table(), "date", ColumnType.DATE, true);
		var result = converter.convert(new BuiltInSelects.DateDistance("value", date, ChronoUnit.YEARS, LocalDate.of(2020, 6, 15)), context());
		assertEquals("min(\"preprocessing\".\"value\") as \"value\"", render(result.getAggregationSelects().getFirst().toFields().getFirst()));
	}

	@Test
	void shouldConvertDistinctWithEventFilteringAndOrderedStringAggregation() {
		var result = converter.convert(new BuiltInSelects.Values("value", VALUE, BuiltInSelects.ValueOperation.DISTINCT, Optional.empty()), context());
		var aggregation = result.getAdditionalPredecessor().orElseThrow();
		var distinct = aggregation.getPredecessors().getFirst();
		assertTrue(distinct.isSelectDistinct());
		assertEquals("value-distinct", distinct.getCteName());
		assertEquals("\"preprocessing\".\"value\"", render(distinct.getSelects().getSqlSelects().getFirst().toFields().getFirst()));
		assertTrue(result.getAggregationSelects().isEmpty());
		assertTrue(render(aggregation.getSelects().getSqlSelects().getFirst().toFields().getFirst()).contains("string_agg("));
	}

	@Test
	void shouldConvertFirstAndLastWithSubstringAndSeparatePredecessor() {
		for (var operation : List.of(BuiltInSelects.ValueOperation.FIRST, BuiltInSelects.ValueOperation.LAST)) {
			var result = converter.convert(new BuiltInSelects.Values("value", VALUE, operation, Optional.of(SubstringRange.between(2, 5))), context());
			assertEquals("substring(\"events\".\"value\", 3, 3) as \"value\"", render(result.getPreprocessingSelects().getFirst().toFields().getFirst()));
			assertEquals("value-value_select_first_row_step", result.getAdditionalPredecessor().orElseThrow().getCteName());
			assertTrue(result.getAggregationSelects().isEmpty());
			assertEquals(1, result.getFinalSelects().size());
		}
	}

	@Test
	void shouldPreserveOpenSubstringBounds() {
		var result = converter.convert(new BuiltInSelects.Values("value", VALUE, BuiltInSelects.ValueOperation.FIRST,
				Optional.of(SubstringRange.from(2))), context());
		assertEquals("substring(\"events\".\"value\", 3) as \"value\"", render(result.getPreprocessingSelects().getFirst().toFields().getFirst()));
	}

	@Test
	void shouldNotAggregateDistinctSumPredecessorAgain() {
		var amount = new ResolvedColumn("events.amount", VALUE.table(), "amount", ColumnType.INTEGER, true);
		var result = converter.convert(new BuiltInSelects.Aggregation("value", new BuiltInAggregations.Sum(amount, Optional.empty(), List.of(VALUE))), context());
		assertTrue(result.getAdditionalPredecessor().isPresent());
		assertTrue(result.getAggregationSelects().isEmpty());
		assertEquals("\"aggregation\".\"value\"", render(result.getFinalSelects().getFirst().toFields().getFirst()));
	}

	@Test
	void shouldConvertRandomWithoutApplyingSubstring() {
		var result = converter.convert(new BuiltInSelects.Values("value", VALUE, BuiltInSelects.ValueOperation.RANDOM,
				Optional.of(SubstringRange.from(2))), context());
		assertEquals("\"events\".\"value\"", render(result.getPreprocessingSelects().getFirst().toFields().getFirst()));
		assertEquals("random_value(\"preprocessing\".\"value\") as \"value\"", render(result.getAggregationSelects().getFirst().toFields().getFirst()));
	}

	private static SelectConversionContext context() {
		return context(List.of());
	}

	private static SelectConversionContext context(List<SelectConversionContext.ConceptColumnSource> conceptColumnSources) {
		return new SelectConversionContext(new TestDialect(), new SqlNameGenerator(127),
				new SqlTables("events", Map.of(ConceptCteStep.PREPROCESSING, "preprocessing", ConceptCteStep.AGGREGATION_SELECT, "aggregation"),
						Map.of(ConceptCteStep.AGGREGATION_SELECT, ConceptCteStep.PREPROCESSING, ConceptCteStep.AGGREGATION_FILTER, ConceptCteStep.AGGREGATION_SELECT)),
				new SqlIdColumns(DSL.field(DSL.name("events", "id"), String.class)), Optional.empty(), "value", Map.of(), conceptColumnSources);
	}

	private static String render(Field<?> field) {
		return DSL.using(SQLDialect.POSTGRES).renderInlined(DSL.select(field)).substring("select ".length()).toLowerCase(Locale.ROOT);
	}

	private static class TestDialect implements CompilerDialect {
		@Override public org.jooq.Condition orAgg(Field<Boolean> field) { return DSL.max(field.cast(Integer.class)).gt(0); }
		@Override public Field<?> arrayOut(List<Field<String>> fields) { return DSL.array(fields); }
		@Override public Field<Integer> dateDistance(ChronoUnit unit, Field<Date> start, Field<Date> end) {
			return DSL.function("date_distance", Integer.class, start, end);
		}
		@Override public Field<Integer> dateDistance(ChronoUnit unit, Field<Date> start, LocalDate end) {
			return dateDistance(unit, start, DSL.inline(Date.valueOf(end)));
		}
		@Override public <T> Field<T> cast(Field<?> field, org.jooq.DataType<T> type) { return field.cast(type); }
		@Override public Field<String> stringAggregation(Field<String> field, Field<String> delimiter, List<Field<?>> order) {
			return DSL.field("string_agg({0}, {1} {2})", String.class, field, delimiter, DSL.orderBy(order));
		}
		@Override public Field<Date> minimumDate() { return DSL.field("minimum_date", Date.class); }
		@Override public Field<Date> maximumDate() { return DSL.field("maximum_date", Date.class); }
		@Override public <T> Field<T> anyValue(Field<T> field) { return field; }
		@Override public <T> Field<T> random(Field<T> field) { return DSL.function("random_value", field.getDataType(), field); }
		@Override public Field<?> renderDateRange(Field<Date> start, Field<Date> end) { return start; }
		@Override public Field<?> aggregateDateRanges(Field<Date> start, Field<Date> end) { return start; }
		@Override public int getNameMaxLength() { return 127; }
	}

	private record UnsupportedSelect(String name) implements ResolvedSelect {
	}
}
