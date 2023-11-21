package com.bakdata.conquery.sql.conversion.model.select;

import lombok.EqualsAndHashCode;
import org.jooq.Field;
import org.jooq.impl.DSL;

@EqualsAndHashCode
public class FieldWrapper<T> implements SqlSelect {

	private final Field<T> field;
	private final String columnName;

	/**
	 * Wrapper for a {@link Field}.
	 * <p>
	 * {@link FieldWrapper#columnName()} will return the given column name of the given field.
	 *
	 * @param field      The field to wrap, e.g. {@code DSL.sum(DSL.field(DSL.name("foo", "bar"))).as("foo_bar")};
	 * @param columnName The "root" column name of the wrapped field, e.g. "bar"
	 */
	public FieldWrapper(Field<T> field, String columnName) {
		this.field = field;
		this.columnName = columnName;
	}

	/**
	 * Wrapper for a {@link Field}.
	 * <p>
	 * {@link FieldWrapper#columnName()} will return the alias of the given field.
	 *
	 * @param field @param field The field to wrap, e.g. {@code DSL.field(DSL.name("fizz", "buzz"))).as("fizz_buzz")};
	 */
	public FieldWrapper(Field<T> field) {
		this.field = field;
		this.columnName = field.getName();
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
	public String columnName() {
		return this.columnName;
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
