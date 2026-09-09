package com.bakdata.conquery.sql.compiler.ir.select;

import java.util.List;
import java.util.Objects;

import org.jooq.Field;
import org.jooq.impl.DSL;

/** A single-column select backed by an arbitrary jOOQ field expression. */
public class FieldWrapper<T> implements SingleColumnSqlSelect {

	private final Field<T> field;
	private final List<String> requiredColumns;

	/**
	 * @param field           field to wrap
	 * @param requiredColumns columns that must be present in the preceding table or CTE
	 */
	public FieldWrapper(Field<T> field, String... requiredColumns) {
		this.field = field;
		this.requiredColumns = List.of(requiredColumns);
	}

	/** Create a wrapper whose required column is derived from the field alias. */
	public FieldWrapper(Field<T> field) {
		this.field = field;
		this.requiredColumns = List.of(field.getName());
	}

	@Override
	public Field<T> select() {
		return field;
	}

	@Override
	public Field<T> aliased() {
		return DSL.field(DSL.name(field.getName()), field.getType());
	}

	@Override
	public List<String> requiredColumns() {
		return requiredColumns;
	}

	@Override
	public ExtractingSqlSelect<T> qualify(String qualifier) {
		Field<T> aliased = aliased();
		return new ExtractingSqlSelect<>(qualifier, aliased.getName(), aliased.getType());
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof FieldWrapper<?> that)) {
			return false;
		}
		return Objects.equals(field, that.field);
	}

	@Override
	public int hashCode() {
		return Objects.hash(field);
	}
}
