package com.bakdata.conquery.sql.conversion.model.select;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Select that does nothing but reference an existing column.
 * <p>
 * This can be used if another select requires a column in a later step.
 *
 * @param <V> type of column
 */
@Value
@EqualsAndHashCode
public class ExtractingSqlSelect<V> implements SqlSelect {

	String table;
	String column;
	@EqualsAndHashCode.Exclude
	Class<V> columnClass;

	@SuppressWarnings("unchecked")
	public static <V> ExtractingSqlSelect<V> fromSqlSelect(SqlSelect select, String qualifier) {
		return (ExtractingSqlSelect<V>) new ExtractingSqlSelect<>(
				qualifier,
				select.columnName(),
				select.aliased().getType()
		);
	}

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
