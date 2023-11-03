package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;

import lombok.EqualsAndHashCode;
import org.jooq.Field;
import org.jooq.impl.DSL;

@EqualsAndHashCode
public class ExistsSqlSelect implements SqlSelect {

	private static final String EXISTS_SUFFIX = "_exists";
	private final String label;

	public ExistsSqlSelect(String label) {
		this.label = label + EXISTS_SUFFIX;
	}

	@Override
	public Field<Integer> select() {
		return DSL.field("1", Integer.class)
				  .as(label);
	}

	@Override
	public Field<Integer> aliased() {
		return DSL.field(label, Integer.class);
	}

	@Override
	public List<String> columnNames() {
		return List.of(label);
	}

}
