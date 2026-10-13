package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.model.node.ExternalEntity;
import com.bakdata.conquery.sql.model.node.ExternalNode;
import com.bakdata.conquery.sql.model.range.DateRange;
import org.jooq.Field;
import org.junit.jupiter.api.Test;

class ExternalQueryStepCompilerTest {

	private static final CompilerDialect DIALECT = new TestDialect();
	private static final DateRange JANUARY = DateRange.closed(
			LocalDate.of(2025, 1, 1),
			LocalDate.of(2025, 1, 31)
	);

	@Test
	void shouldCompileOneEntityRowForEveryValidityRange() {
		ExternalNode node = new ExternalNode(
				List.of(
						new ExternalEntity("person-1", List.of(JANUARY, DateRange.closed(
								LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31)
						)), Map.of()),
						new ExternalEntity("person-2", List.of(), Map.of())
				),
				List.of()
		);

		ExternalQuerySteps compiled = ExternalQueryStepCompiler.compile(node, true, Optional.empty(), DIALECT);

		assertEquals("external_ids", compiled.entities().getCteName());
		assertEquals(2, compiled.entities().getUnion().size());
		assertTrue(compiled.entities().isNegate());
		assertTrue(compiled.entities().getSelects().getValidityDate().isPresent());
		assertTrue(compiled.values().isEmpty());
	}

	@Test
	void shouldCompileOrderedExternalValueColumnsForEveryEntity() {
		ExternalNode node = new ExternalNode(
				List.of(
						new ExternalEntity("person-1", List.of(JANUARY), Map.of("group name", List.of("A", "B"))),
						new ExternalEntity("person-2", List.of(JANUARY), Map.of())
				),
				List.of("group name")
		);

		QueryStep values = ExternalQueryStepCompiler.compile(node, false, Optional.empty(), DIALECT)
				.values()
				.orElseThrow();

		assertEquals("external_extra", values.getCteName());
		assertEquals(1, values.getUnion().size());
		assertEquals(List.of("group_name"), values.getSelects().explicitSelects().stream().map(Field::getName).toList());
		assertEquals(List.of("group_name"), values.getUnion().getFirst().getSelects().explicitSelects().stream()
				.map(Field::getName)
				.toList());
	}

	@Test
	void shouldApplyResolvedDateRestrictionInASeparateStep() {
		ExternalNode node = new ExternalNode(
				List.of(new ExternalEntity("person-1", List.of(JANUARY), Map.of())),
				List.of()
		);

		QueryStep restricted = ExternalQueryStepCompiler.compile(
				node,
				false,
				Optional.of(DateRange.closed(LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 20))),
				DIALECT
		).entities();

		assertEquals("external_ids_date_restriction", restricted.getCteName());
		assertEquals(List.of("external_ids"), restricted.getPredecessors().stream().map(QueryStep::getCteName).toList());
		assertEquals(1, restricted.getConditions().size());
		assertFalse(restricted.isNegate());
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
