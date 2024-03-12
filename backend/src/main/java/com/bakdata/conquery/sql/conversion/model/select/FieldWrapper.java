package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;

import lombok.EqualsAndHashCode;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Wrapper for a {@link Field}.
 */
@EqualsAndHashCode
public class FieldWrapper<T> implements SingleColumnSqlSelect {

	private final Field<T> field;
	@EqualsAndHashCode.Exclude
	private final List<String> requiredColumns;

	/**
	 * @param field           The field to wrap, e.g. {@code DSL.sum(DSL.field(DSL.name("foo", "bar"))).as("foo_bar")};
	 * @param requiredColumns All columns this {@link FieldWrapper} requires in the previous CTE/table to be present.
	 */
	public FieldWrapper(Field<T> field, String... requiredColumns) {
		this.field = field;
		this.requiredColumns = List.of(requiredColumns);
	}

	/**
	 * @param field @param field The field to wrap, e.g. {@code DSL.field(DSL.name("fizz", "buzz"))).as("fizz_buzz")}; The column for
	 * {@link FieldWrapper#requiredColumns()} will be taken from the alias of the wrapped field.
	 */
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
		return this.requiredColumns;
	}

	@Override
	public ExtractingSqlSelect<T> qualify(String qualifier) {
		Field<T> aliased = aliased();
		return new ExtractingSqlSelect<>(
				qualifier,
				aliased.getName(),
				aliased.getType()
		);
	}

}
