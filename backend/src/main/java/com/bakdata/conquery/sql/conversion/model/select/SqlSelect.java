package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;

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
	 * All column names this {@link SqlSelect} requires to build its {@link SqlSelect#select()}.
	 */
	List<String> requiredColumns();

	/**
	 * @return Creates a reference to the alias of this SqlSelect qualified onto the given qualifier.
	 */
	ExtractingSqlSelect<?> createAliasReference(String qualifier);

	/**
	 * @return Determines if this is only part of the {@link ConnectorCteStep#FINAL} CTE and has no predeceasing selects.
	 */
	default boolean isUniversal() {
		return false;
	}

}
