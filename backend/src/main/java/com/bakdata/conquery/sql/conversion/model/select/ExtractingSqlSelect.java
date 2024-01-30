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
 * @param <T> type of column
 */
@Value
@EqualsAndHashCode
public class ExtractingSqlSelect<T> implements SqlSelect {

	String table;
	String column;
	@EqualsAndHashCode.Exclude
	Class<T> columnClass;

	@Override
	public Field<T> select() {
		return DSL.field(DSL.name(table, column), columnClass);
	}

	@Override
	public Field<T> aliased() {
		return DSL.field(DSL.name(column), columnClass);
	}

	@Override
	public String columnName() {
		return column;
	}

	@Override
	public ExtractingSqlSelect<T> createAliasedReference(String qualifier) {
		Field<T> aliased = aliased();
		return new ExtractingSqlSelect<>(
				qualifier,
				aliased.getName(),
				aliased.getType()
		);
	}

}
