package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;

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
public class ExtractingSqlSelect<T> implements SingleColumnSqlSelect {

	String table;
	String column;
	Class<T> columnClass;
	boolean isUniversal;

	public ExtractingSqlSelect(String table, String column, Class<T> columnClass) {
		this.table = table;
		this.column = column;
		this.columnClass = columnClass;
		this.isUniversal = false;
	}

	public ExtractingSqlSelect(String table, String column, Class<T> columnClass, boolean isUniversal) {
		this.table = table;
		this.column = column;
		this.columnClass = columnClass;
		this.isUniversal = isUniversal;
	}

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
	public boolean isUniversal() {
		return isUniversal;
	}

	@Override
	public ExtractingSqlSelect<T> qualify(String qualifier) {
		Field<T> aliased = aliased();
		return new ExtractingSqlSelect<>(
				qualifier,
				aliased.getName(),
				aliased.getType(),
				isUniversal
		);
	}

}
