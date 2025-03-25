package com.bakdata.conquery.sql.conversion.model.select;

import org.jooq.Field;

public class UniversalSqlSelect<T> extends FieldWrapper<T> {

	public UniversalSqlSelect(Field<T> field) {
		super(field);
	}

	@Override
	public boolean isUniversal() {
		return true;
	}

	@Override
	public ExtractingSqlSelect<T> qualify(final String qualifier) {
		Field<T> aliased = aliased();
		return new ExtractingSqlSelect<>(
				qualifier,
				aliased.getName(),
				aliased.getType(),
				true
		);
	}
}
