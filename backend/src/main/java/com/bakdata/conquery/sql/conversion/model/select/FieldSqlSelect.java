package com.bakdata.conquery.sql.conversion.model.select;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode
public class FieldSqlSelect implements SqlSelect {

	private final Field<?> field;

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