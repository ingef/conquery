package com.bakdata.conquery.sql.compiler.ir.select;

import java.util.List;

import org.jooq.Field;

/** Intermediate select represented by one SQL field. */
public interface SingleColumnSqlSelect extends SqlSelect {

	/**
	 * @return the complete aliased SQL expression, for example
	 * {@code DSL.firstValue(DSL.field(DSL.name("foo", "bar"))).as("foobar")}
	 */
	Field<?> select();

	/**
	 * @return the aliased column reference, for example {@code DSL.field("foobar")}
	 */
	Field<?> aliased();

	@Override
	SingleColumnSqlSelect qualify(String qualifier);

	@Override
	default List<Field<?>> toFields() {
		return List.of(select());
	}
}
