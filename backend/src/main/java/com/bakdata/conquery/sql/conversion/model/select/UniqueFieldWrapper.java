package com.bakdata.conquery.sql.conversion.model.select;

import lombok.Getter;
import org.jooq.Field;

@Getter
public class UniqueFieldWrapper extends FieldWrapper implements ExplicitSelect {

	private final SqlSelectId sqlSelectId;

	private UniqueFieldWrapper(Field<?> field, SqlSelectId sqlSelectId) {
		super(field);
		this.sqlSelectId = sqlSelectId;
	}

	/**
	 * @return Aliases an existing {@link SqlSelect} with a unique alias.
	 */
	public static UniqueFieldWrapper create(ExplicitSelect sqlSelect) {
		Field<?> field = sqlSelect.select();
		return new UniqueFieldWrapper(
				field.as("%s-%8X".formatted(field.getName(), field.hashCode())),
				sqlSelect.getSqlSelectId()
		);
	}

}
