package com.bakdata.conquery.sql.conversion.model;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class QualifyingUtil {

	public static <T> Field<T> qualify(Field<T> field, String qualifier) {
		return DSL.field(DSL.name(qualifier, field.getName()), field.getType());
	}

	public static <T extends SqlSelect> List<T> qualify(List<T> sqlSelects, String qualifier, Class<T> selectClass) {
		return sqlSelects.stream()
						 .flatMap(sqlSelect -> sqlSelect.createReferences(qualifier, selectClass).stream())
						 .collect(Collectors.toList());
	}

}
