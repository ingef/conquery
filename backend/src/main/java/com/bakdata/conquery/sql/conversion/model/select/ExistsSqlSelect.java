package com.bakdata.conquery.sql.conversion.model.select;

import java.util.Collections;
import java.util.List;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;

public class ExistsSqlSelect extends UniversalSqlSelect<Integer> {

	private static final Field<Integer> EXISTS = DSL.val(1);

	public ExistsSqlSelect(String alias) {
		super(EXISTS.as(alias));
	}

	@Override
	public List<String> requiredColumns() {
		return Collections.emptyList();
	}

}
