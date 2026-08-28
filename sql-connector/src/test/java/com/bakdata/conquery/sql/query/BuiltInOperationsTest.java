package com.bakdata.conquery.sql.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.bakdata.conquery.sql.query.ValidationTestSupport.assertInvalid;
import static com.bakdata.conquery.sql.query.ValidationTestSupport.assertValid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.query.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.query.operation.BuiltInConditions;
import com.bakdata.conquery.sql.query.operation.BuiltInFilters;
import com.bakdata.conquery.sql.query.operation.BuiltInSelects;
import com.bakdata.conquery.sql.query.operation.ResolvedCondition;
import com.bakdata.conquery.sql.query.operation.ResolvedFilter;
import com.bakdata.conquery.sql.query.operation.ResolvedSelect;
import com.bakdata.conquery.sql.query.range.NumberRange;
import com.bakdata.conquery.sql.query.range.SubstringRange;
import com.bakdata.conquery.sql.query.schema.DateColumns;
import com.bakdata.conquery.sql.query.schema.ResolvedColumn;
import com.bakdata.conquery.sql.query.schema.SqlTable;
import org.junit.jupiter.api.Test;

class BuiltInOperationsTest {

	private static final SqlTable EVENTS = SqlTable.of("events", "analytics", "events");
	private static final SqlTable OTHER_EVENTS = SqlTable.of("other-events", "analytics", "other_events");
	private static final ResolvedColumn CODE = column("code", ColumnType.STRING);
	private static final ResolvedColumn AMOUNT = column("amount", ColumnType.DECIMAL);
	private static final ResolvedColumn START = column("start_date", ColumnType.DATE);
	private static final ResolvedColumn END = column("end_date", ColumnType.DATE);
	private static final ResolvedColumn FLAG_A = column("flag_a", ColumnType.BOOLEAN);
	private static final ResolvedColumn FLAG_B = column("flag_b", ColumnType.BOOLEAN);

	@Test
	void shouldRepresentStandardFiltersWithResolvedInputs() {
		ResolvedFilter values = new BuiltInFilters.StringValues(
				CODE, Set.of("A", "B"), Optional.of(SubstringRange.between(0, 3))
		);
		ResolvedFilter number = new BuiltInFilters.NumericColumnRange(AMOUNT, NumberRange.closed(1, 10));
		ResolvedFilter aggregation = new BuiltInFilters.AggregationRange(
				new BuiltInAggregations.Sum(AMOUNT, Optional.empty(), List.of()),
				NumberRange.atLeast(BigDecimal.TEN)
		);
		ResolvedFilter distance = new BuiltInFilters.DateDistanceRange(
				START, ChronoUnit.YEARS, LocalDate.of(2026, 8, 27), NumberRange.atMost(65)
		);
		ResolvedFilter flags = new BuiltInFilters.Flags(
				Map.of("A", FLAG_A, "B", FLAG_B), Set.of("A")
		);

		assertValid(values);
		assertValid(number);
		assertValid(aggregation);
		assertValid(distance);
		assertValid(flags);
		assertInstanceOf(BuiltInFilters.StringValues.class, values);
		assertInstanceOf(BuiltInFilters.NumericColumnRange.class, number);
		assertInstanceOf(BuiltInFilters.AggregationRange.class, aggregation);
		assertInstanceOf(BuiltInFilters.DateDistanceRange.class, distance);
		assertInstanceOf(BuiltInFilters.Flags.class, flags);
	}

	@Test
	void shouldRepresentStandardSelectsWithResolvedInputs() {
		List<ResolvedSelect> selects = List.of(
				new BuiltInSelects.Aggregation(new BuiltInAggregations.Count(CODE, List.of(CODE))),
				new BuiltInSelects.Aggregation(new BuiltInAggregations.CountQuarters(new DateColumns.Pair(START, END))),
				new BuiltInSelects.Aggregation(new BuiltInAggregations.DurationSum(new DateColumns.Pair(START, END), List.of(CODE))),
				new BuiltInSelects.Aggregation(new BuiltInAggregations.Flags(Map.of("A", FLAG_A, "B", FLAG_B))),
				new BuiltInSelects.Values(
						CODE, BuiltInSelects.ValueOperation.DISTINCT, Optional.of(SubstringRange.between(0, 3))
				),
				new BuiltInSelects.DateUnion(new DateColumns.Pair(START, END)),
				new BuiltInSelects.DateDistance(START, ChronoUnit.DAYS, LocalDate.of(2026, 8, 27)),
				new BuiltInSelects.ConceptValues(List.of(CODE)),
				new BuiltInSelects.EventDateUnion(),
				new BuiltInSelects.EventDurationSum(),
				new BuiltInSelects.Exists()
		);

		selects.forEach(ValidationTestSupport::assertValid);
		assertEquals(11, selects.size());
	}

	@Test
	void shouldRepresentDeclarativeConditions() {
		ResolvedCondition condition = new BuiltInConditions.AllOf(List.of(
				new BuiltInConditions.StringValues(CODE, Set.of("A")),
				new BuiltInConditions.Presence(AMOUNT, true),
				new BuiltInConditions.Prefixes(CODE, List.of("AB", "CD")),
				new BuiltInConditions.Not(new BuiltInConditions.PrefixRange(CODE, "10", "20"))
		));

		assertValid(condition);
		assertEquals(4, assertInstanceOf(BuiltInConditions.AllOf.class, condition).conditions().size());
	}

	@Test
	void shouldDefensivelyCopyOperationCollections() {
		List<ResolvedColumn> distinctBy = new ArrayList<>(List.of(CODE));
		BuiltInAggregations.Count count = new BuiltInAggregations.Count(AMOUNT, distinctBy);
		distinctBy.clear();

		Map<String, ResolvedColumn> flagColumns = new LinkedHashMap<>();
		flagColumns.put("A", FLAG_A);
		flagColumns.put("B", FLAG_B);
		BuiltInAggregations.Flags flags = new BuiltInAggregations.Flags(flagColumns);
		flagColumns.clear();

		assertEquals(List.of(CODE), count.distinctBy());
		assertEquals(List.of("A", "B"), new ArrayList<>(flags.columns().keySet()));
		assertThrows(UnsupportedOperationException.class, () -> flags.columns().clear());
	}

	@Test
	void shouldRejectColumnsFromDifferentTablesInOneOperation() {
		ResolvedColumn otherColumn = new ResolvedColumn(
				"other.code", OTHER_EVENTS, "code", ColumnType.STRING, true
		);

		assertInvalid(new BuiltInAggregations.Count(CODE, List.of(otherColumn)));
		assertInvalid(new DateColumns.Pair(START, new ResolvedColumn(
						"other.end", OTHER_EVENTS, "end_date", ColumnType.DATE, true
		)));
	}

	@Test
	void shouldRejectInvalidColumnTypes() {
		assertInvalid(new BuiltInAggregations.Sum(CODE, Optional.empty(), List.of()));
		assertInvalid(new BuiltInFilters.DateDistanceRange(
						CODE, ChronoUnit.DAYS, LocalDate.of(2026, 8, 27), NumberRange.unbounded()
		));
		assertInvalid(new BuiltInSelects.Values(
						AMOUNT, BuiltInSelects.ValueOperation.FIRST, Optional.of(SubstringRange.from(1))
		));
	}

	@Test
	void shouldRejectUnknownSelectedFlags() {
		assertInvalid(new BuiltInFilters.Flags(Map.of("A", FLAG_A), Set.of("missing")));
	}

	@Test
	void shouldRejectInvalidRanges() {
		assertInvalid(NumberRange.closed(2, 1));
		assertInvalid(SubstringRange.between(3, 2));
		assertInvalid(new BuiltInConditions.PrefixRange(CODE, "100", "20"));
	}

	private static ResolvedColumn column(String name, ColumnType type) {
		return new ResolvedColumn("events." + name, EVENTS, name, type, true);
	}
}
