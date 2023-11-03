package com.bakdata.conquery.sql.conversion.model.select;


import java.util.List;

import org.jooq.Field;

public interface SqlSelect {

	/**
	 * @return The whole (aliased) SQL expression of this {@link SqlSelect}.
	 * For example, {@code DSL.firstValue(DSL.field(DSL.name("foo", "bar"))).as("foobar")}.
	 */
	Field<?> select();

	/**
	 * @return Aliased column name that can be used to reference the created select.
	 * For example, {@code DSL.field("foobar")}.
	 */
	Field<?> aliased();

	/**
	 * @return Plain column name of this {@link SqlSelect}.
	 * For example, {@code "bar"}.
	 */
	List<String> columnNames();

	/**
	 * @return A reference to this {@link SqlSelect} qualified on the given qualifier.
	 */
	default <T extends SqlSelect> List<T> createReferences(String qualifier, Class<T> selectClass) {
		return columnNames().stream()
							.map(columnName -> createReferences(qualifier, selectClass, columnName))
							.toList();
	}

	private <T extends SqlSelect> T createReferences(String qualifier, Class<T> selectClass, String columnName) {
		return selectClass.cast(
				new ExtractingSqlSelect<>(
						qualifier,
						columnName,
						aliased().getType()
				)
		);
	}

}
