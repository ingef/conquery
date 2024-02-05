package com.bakdata.conquery.sql.conversion.model.select;


import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
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
	String columnName();

	/**
	 * @return Creates a reference to the alias of this SqlSelect qualified onto the given qualifier.
	 */
	ExtractingSqlSelect<?> createAliasedReference(String qualifier);
	/**
	 * @return Creates a reference to the column of this SqlSelect qualified onto the given qualifier.
	 */
	default ExtractingSqlSelect<?> createColumnReference(String qualifier) {
		return new ExtractingSqlSelect<>(
				qualifier,
				columnName(),
				Object.class
		);
	}

	/**
	 * @return Determines if this is only part of the {@link ConnectorCteStep#FINAL} CTE and has no predeceasing selects.
	 */
	default boolean isUniversal() {
		return false;
	}

}
