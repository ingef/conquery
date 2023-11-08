package com.bakdata.conquery.sql.conversion.model.select;


import org.jooq.Field;

public interface SqlSelect {

	/**
	 * @return The whole (aliased) SQL expression of this {@link SqlSelect}.
	 * 	For example, {@code DSL.firstValue(DSL.field(DSL.name("foo", "bar"))).as("foobar")}.
	 */
	Field<?> select();

	/**
	 * @return Aliased column name that can be used to reference the created select.
	 *  For example, {@code DSL.field("foobar")}.
	 */
	Field<?> aliased();

	/**
	 * @return Plain column name of this {@link SqlSelect}.
	 * For example, {@code "bar"}.
	 */
	String columnName();

}
