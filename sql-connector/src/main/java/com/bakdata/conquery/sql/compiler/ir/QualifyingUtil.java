package com.bakdata.conquery.sql.compiler.ir;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import org.jooq.Field;
import org.jooq.impl.DSL;

/** Utilities for rebinding compiler IR fields and selects to a table or CTE qualifier. */
public final class QualifyingUtil {

	private QualifyingUtil() {
	}

	public static <T> Field<T> qualify(Field<T> field, String qualifier) {
		return DSL.field(DSL.name(qualifier, field.getName()), field.getType());
	}

	public static List<SqlSelect> qualify(List<SqlSelect> sqlSelects, String qualifier) {
		return sqlSelects.stream()
				.map(sqlSelect -> sqlSelect.qualify(qualifier))
				.collect(Collectors.toList());
	}
}
