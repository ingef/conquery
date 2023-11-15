package com.bakdata.conquery.sql.conversion.model.select;

import lombok.EqualsAndHashCode;
import org.jooq.Field;
import org.jooq.impl.DSL;

@EqualsAndHashCode
public class ExistsSqlSelect implements SqlSelect {

	private static final String EXISTS_SUFFIX = "_exists";

	private final String alias;

	public ExistsSqlSelect(String alias) {
		this.alias = alias + EXISTS_SUFFIX;
	}

	@Override
	public Field<Integer> select() {
		return DSL.field("1", Integer.class)
				  .as(alias);
	}

	@Override
	public Field<Integer> aliased() {
		return DSL.field(alias, Integer.class);
	}

	@Override
	public String columnName() {
		return alias;
	}

}
