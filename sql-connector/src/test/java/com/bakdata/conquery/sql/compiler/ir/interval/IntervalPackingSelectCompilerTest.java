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

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
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
}
