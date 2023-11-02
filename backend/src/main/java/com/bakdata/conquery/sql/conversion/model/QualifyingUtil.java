package com.bakdata.conquery.sql.conversion.model;

import org.jooq.Field;
import org.jooq.impl.DSL;

public class QualifyingUtil {

	public static <T> Field<T> qualify(Field<T> field, String qualifier) {
		return DSL.field(DSL.name(qualifier, field.getName()), field.getType());
	}

}
