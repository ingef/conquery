package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;

import lombok.EqualsAndHashCode;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Wrapper for a {@link Field}.
 */
@EqualsAndHashCode
public class FieldWrapper<T> implements SqlSelect {

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
	 * @param field       The field to wrap, e.g. {@code DSL.sum(DSL.field(DSL.name("foo", "bar"))).as("foo_bar")};
	 * @param predecessor The {@link ExtractingSqlSelect} containing the required column this {@link FieldWrapper} uses.
	 */
	public FieldWrapper(Field<T> field, ExtractingSqlSelect<?> predecessor) {
		this.field = field;
		this.requiredColumns = predecessor.requiredColumns();
	}


	/**
	 * Wrapper for a {@link Field}.
	 * <p>
	 *
	 * @param field @param field The field to wrap, e.g. {@code DSL.field(DSL.name("fizz", "buzz"))).as("fizz_buzz")};
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
	public ExtractingSqlSelect<T> createAliasReference(String qualifier) {
		Field<T> aliased = aliased();
		return new ExtractingSqlSelect<>(
				qualifier,
				aliased.getName(),
				aliased.getType()
		);
	}

}
