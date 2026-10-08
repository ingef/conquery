package com.bakdata.conquery.sql.compiler.ir.select;

import java.util.List;

import org.jooq.Field;
import org.jooq.impl.DSL;

/** A single-column select that references an existing column. */
public record ExtractingSqlSelect<T>(String table, String column, Class<T> columnClass) implements SingleColumnSqlSelect {

	@Override
	public Field<T> select() {
		return DSL.field(DSL.name(table, column), columnClass);
	}

	@Override
	public Field<T> aliased() {
		return DSL.field(DSL.name(column), columnClass);
	}

	@Override
	public List<String> requiredColumns() {
		return List.of(column);
	}

	@Override
	public ExtractingSqlSelect<T> qualify(String qualifier) {
		Field<T> aliased = aliased();
		return new ExtractingSqlSelect<>(qualifier, aliased.getName(), aliased.getType());
	}
}
