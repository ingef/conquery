package com.bakdata.conquery.sql.conversion.model.select;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode
public class FieldWrapper implements SqlSelect {

	private final Field<?> field;

	/**
	 * @return Aliases an existing {@link SqlSelect} with a unique alias.
	 */
	public static FieldWrapper unique(SqlSelect sqlSelect) {
		Field<?> field = sqlSelect.select();
		return new FieldWrapper(field.as("%s-%8X".formatted(field.getName(), field.hashCode())));
	}

	@Override
	public Field<?> select() {
		return field;
	}

	@Override
	public Field<?> aliased() {
		return DSL.field(field.getName());
	}

	@Override
	public String columnName() {
		return field.getName();
	}

}
