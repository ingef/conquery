package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import org.junit.jupiter.api.Test;

class QueryStepTest {

	@Test
	void shouldQualifyItsProjectionByCteName() {
		QueryStep step = QueryStep.builder()
				.cteName("filtered")
				.selects(selects())
				.build();

		Selects qualified = step.getQualifiedSelects();

		assertEquals(name("filtered", "person"), qualified.getIds().getPrimaryColumn().getQualifiedName());
		assertEquals(name("filtered", "value"), qualified.explicitSelects().getFirst().getQualifiedName());
	}

	@Test
	void shouldAddAnExplicitSelectWithoutChangingTheOriginalStep() {
		QueryStep original = QueryStep.builder()
				.cteName("source")
				.selects(Selects.builder()
						.ids(new SqlIdColumns(field(name("person"), String.class)))
						.build())
				.build();

		QueryStep extended = original.addSqlSelect(new FieldWrapper<>(field(name("value"), Integer.class)));

		assertTrue(original.getSelects().getSqlSelects().isEmpty());
		assertEquals(List.of("value"), extended.getSelects().explicitSelects().stream().map(org.jooq.Field::getName).toList());
	}

	@Test
	void shouldCreateUnionAllStepFromFirstProjection() {
		QueryStep first = QueryStep.builder()
				.cteName("first")
				.selects(selects())
				.projectionMode(ProjectionMode.INDIVIDUAL)
				.build();
		QueryStep second = QueryStep.builder().cteName("second").selects(selects()).build();
		QueryStep predecessor = QueryStep.builder().cteName("predecessor").selects(selects()).build();

		QueryStep union = QueryStep.createUnionAllStep(
				List.of(first, second),
				"combined",
				List.of(predecessor),
				true
		);

		assertEquals("combined", union.getCteName());
		assertEquals(List.of(second), union.getUnion());
		assertEquals(List.of(predecessor), union.getPredecessors());
		assertTrue(union.isUnion());
		assertTrue(union.isUnionAll());
		assertTrue(union.isNegate());
		assertFalse(union.isGroupBy());
		assertEquals(ProjectionMode.INDIVIDUAL, union.getProjectionMode());
	}

	private static Selects selects() {
		return Selects.builder()
				.ids(new SqlIdColumns(field(name("person"), String.class)))
				.sqlSelect(new FieldWrapper<>(field(name("value"), Integer.class)))
				.build();
	}
}
