package com.bakdata.conquery.sql.compiler.ir;

import java.util.List;

import lombok.experimental.UtilityClass;
import org.jooq.Field;
import org.jooq.impl.DSL;

/** Dialect-independent field expressions used while composing compiler IR. */
@UtilityClass
public class FieldExpressions {

	public <T> Field<T> least(List<Field<T>> fields) {
		return function("least", fields);
	}

	public <T> Field<T> greatest(List<Field<T>> fields) {
		return function("greatest", fields);
	}

	@SuppressWarnings("unchecked")
	private <T> Field<T> function(String name, List<Field<T>> fields) {
		if (fields.isEmpty()) {
			return null;
		}

		Field<T>[] fieldArray = fields.toArray(Field[]::new);
		return DSL.function(name, fieldArray[0].getType(), fieldArray);
	}
}
