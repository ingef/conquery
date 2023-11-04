package com.bakdata.conquery.sql.conversion.model.select;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Select that does nothing but reference an existing column.
 * <p>
 * This can be used if another select requires a column in a later step.
 *
 * @param <V> type of column
 */
@AllArgsConstructor
@EqualsAndHashCode
public class ExtractingSqlSelect<V> implements SqlSelect {

	private final String table;
	private final String column;
	@EqualsAndHashCode.Exclude
	private final Class<V> columnClass;

	@Override
	public Field<V> select() {
		return DSL.field(DSL.name(table, column), columnClass);
	}

	@Override
	public Field<V> aliased() {
		return DSL.field(DSL.name(column), columnClass);
	}

	@Override
	public String columnName() {
		return column;
	}

}
