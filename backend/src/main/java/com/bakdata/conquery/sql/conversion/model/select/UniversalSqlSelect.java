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
}
