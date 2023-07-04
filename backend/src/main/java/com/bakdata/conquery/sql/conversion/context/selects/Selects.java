package com.bakdata.conquery.sql.conversion.context.selects;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jooq.Field;
import org.jooq.impl.DSL;

public interface Selects {

	Field<Object> getPrimaryColumn();

	Optional<Field<Object>> getValidityDate();

	/**
	 * Returns the selected columns as fully qualified reference.
	 *
	 * @param qualifier the table name that creates these selects
	 * @return selects as fully qualified reference
	 * @see Selects#mapFieldToQualifier(String, Field)
	 */
	Selects byName(String qualifier);

	/**
	 * @return A list of all select fields including the primary column and validity date.
	 */
	List<Field<Object>> all();

	/**
	 * List of columns that the user explicitly referenced, either via a filter or a select.
	 *
	 * @return A list of all select fields WITHOUT implicitly selected columns like the primary column and validity date.
	 */
	List<Field<Object>> explicitSelects();

	default Stream<Field<Object>> mapFieldStreamToQualifier(String qualifier, Stream<Field<Object>> objectField) {
		return objectField.map(column -> this.mapFieldToQualifier(qualifier, column));
	}

	/**
	 * Converts a select to its fully qualified reference.
	 *
	 * <p>
	 * <h3>Example:</h3>
	 * <pre>{@code
	 * with a as (select c1 - c2 as c
	 * from t1)
	 * select t1.c
	 * from a
	 * }</pre>
	 * <p>
	 * This function maps the select {@code c1 - c2 as c} to {@code t1.c}.
	 *
	 * @param qualifier
	 * @param field
	 * @return
	 */
	default Field<Object> mapFieldToQualifier(String qualifier, Field<Object> field) {
		return DSL.field(DSL.name(qualifier, field.getName()));
	}

}
