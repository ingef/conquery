package com.bakdata.conquery.sql.compiler.ir.interval;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import org.jooq.Field;
import org.junit.jupiter.api.Test;

class IntervalPackingSelectCompilerTest {

	private static final SqlTables TABLES = new SqlTables(
			"source",
			Map.of(ConceptCteStep.INTERVAL_PACKING_SELECTS, "interval_selects"),
			Map.of()
	);

	@Test
	void shouldReturnPredecessorWhenNoSelectRequiresIntervalPacking() {
		QueryStep predecessor = predecessor(true);

		QueryStep result = IntervalPackingSelectCompiler.compile(predecessor, List.of(), TABLES);

		assertSame(predecessor, result);
	}

	@Test
	void shouldRequireValidityDate() {
		QueryStep predecessor = predecessor(false);

		assertThrows(
				IllegalArgumentException.class,
				() -> IntervalPackingSelectCompiler.compile(
						predecessor,
						List.of(new FieldWrapper<>(field(name("event_duration"), Integer.class))),
						TABLES
				)
		);
	}

	@Test
	void shouldCompileIntervalPackingSelectBranch() {
		QueryStep predecessor = predecessor(true);
		FieldWrapper<Integer> eventDuration = new FieldWrapper<>(field(name("event_duration"), Integer.class));

		QueryStep result = IntervalPackingSelectCompiler.compile(predecessor, List.of(eventDuration), TABLES);

		assertEquals("interval_selects", result.getCteName());
		assertEquals(List.of(eventDuration), result.getSelects().getSqlSelects());
		assertEquals(
				List.of(name("interval_complete", "person")),
				result.getGroupBy().stream().map(Field::getQualifiedName).toList()
		);
		assertEquals("\"interval_complete\"", result.getFromTables().getFirst().toString());
	}

	@Test
	void shouldCompileArbitraryIntervalSelect() {
		QueryStep predecessor = predecessor(true);
		ColumnDateRange dateRange = predecessor.getSelects().getValidityDate().orElseThrow();
		FieldWrapper<Integer> duration = new FieldWrapper<>(field(name("duration"), Integer.class));
		IntervalPackingSelectPreparation preparation = IntervalPackingSelectCompiler.prepareArbitrarySelect(
				"duration",
				"source",
				predecessor.getSelects().getIds(),
				dateRange,
				new TestDialect(false),
				new SqlNameGenerator(128)
		);

		QueryStep result = IntervalPackingSelectCompiler.compileArbitrarySelect(
				preparation.predecessor(),
				preparation.dateRange(),
				duration,
				preparation.tables(),
				new TestDialect(false)
		);

		assertEquals("duration-interval_packing_selects", result.getCteName());
		assertEquals(List.of(preparation.predecessor()), result.getPredecessors());
		assertEquals("duration-interval_complete", preparation.predecessor().getCteName());
		assertEquals(name("duration-interval_complete", "valid_from"), preparation.dateRange().getStart().getQualifiedName());
		assertEquals(List.of(duration), result.getSelects().getSqlSelects());
		assertEquals("\"duration-interval_complete\"", result.getFromTables().getFirst().toString());
	}

	@Test
	void shouldExpandSingleColumnRangeBeforeCompilingArbitrarySelect() {
		QueryStep predecessor = predecessor(true);
		ColumnDateRange dateRange = predecessor.getSelects().getValidityDate().orElseThrow();
		FieldWrapper<Integer> duration = new FieldWrapper<>(field(name("duration"), Integer.class));
		SqlTables tables = new SqlTables(
				"source",
				Map.of(
						ConceptCteStep.UNNEST_DATE, "unnested",
						ConceptCteStep.INTERVAL_PACKING_SELECTS, "interval_selects"
				),
				Map.of()
		);

		QueryStep result = IntervalPackingSelectCompiler.compileArbitrarySelect(
				predecessor,
				dateRange,
				duration,
				tables,
				new TestDialect(true)
		);

		QueryStep unnested = result.getPredecessors().getLast();
		assertEquals(List.of(predecessor, unnested), result.getPredecessors());
		assertEquals("unnested", unnested.getCteName());
		assertEquals("\"unnested\"", result.getFromTables().getFirst().toString());
	}

	private static QueryStep predecessor(boolean withValidityDate) {
		Selects.SelectsBuilder selects = Selects.builder()
				.ids(new SqlIdColumns(field(name("person"), String.class)));
		if (withValidityDate) {
			selects.validityDate(Optional.of(ColumnDateRange.of(
					field(name("valid_from"), Date.class),
					field(name("valid_to"), Date.class)
			)));
		}
		return QueryStep.builder()
				.cteName("interval_complete")
				.selects(selects.build())
				.build();
	}

	private record TestDialect(boolean supportsSingleColumnRanges) implements CompilerDialect {

		@Override
		public QueryStep unnestDateRange(ColumnDateRange dateRange, QueryStep predecessor, String cteName) {
			return predecessor.toBuilder()
					.cteName(cteName)
					.predecessors(List.of(predecessor))
					.build();
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
