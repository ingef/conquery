package com.bakdata.conquery.sql.conversion.model.select;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode
public class ExistsSqlSelect implements SqlSelect {

	private final String label;

	@Override
	public Field<Integer> select() {
		return DSL.field("1", Integer.class)
				  .as(label + "_exists");
	}

	@Override
	public Field<Integer> aliased() {
		return DSL.field(label + "_exists", Integer.class);
	}

	@Override
	public String columnName() {
		return label;
	}

}
