package com.bakdata.conquery.sql.compiler.ir.select;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jooq.Field;
import org.junit.jupiter.api.Test;

class SqlSelectImplementationsTest {

	@Test
	void shouldDeriveRequiredColumnFromFieldAlias() {
		Field<Integer> field = field(name("source"), Integer.class).as("result");

		FieldWrapper<Integer> select = new FieldWrapper<>(field);

		assertEquals(List.of("result"), select.requiredColumns());
		assertEquals("result", select.aliased().getName());
	}

	@Test
	void shouldKeepFieldWrapperIdentityIndependentOfRequiredColumns() {
		Field<Integer> field = field(name("source"), Integer.class).as("result");

		assertEquals(new FieldWrapper<>(field, "first"), new FieldWrapper<>(field, "second"));
	}

	@Test
	void shouldQualifyExtractingSelects() {
		ExtractingSqlSelect<Integer> select = new ExtractingSqlSelect<>("source", "result", Integer.class);

		ExtractingSqlSelect<Integer> qualified = select.qualify("next_step");

		assertEquals("next_step", qualified.table());
		assertEquals("result", qualified.column());
		assertEquals(Integer.class, qualified.columnClass());
		assertEquals(name("next_step", "result"), qualified.select().getQualifiedName());
	}

	@Test
	void shouldRepresentExistenceAsUniversalSelect() {
		ExistsSqlSelect select = ExistsSqlSelect.withAlias("present");

		assertTrue(select.isUniversal());
		assertEquals(List.of(), select.requiredColumns());
		assertEquals("present", select.select().getName());
		assertEquals(name("next_step", "present"), select.qualify("next_step").select().getQualifiedName());
		assertEquals("present", select.connectorAggregate().toFields().getFirst().getName());
		assertEquals("present", select.toFinalRepresentation().toFields().getFirst().getName());
	}
}
