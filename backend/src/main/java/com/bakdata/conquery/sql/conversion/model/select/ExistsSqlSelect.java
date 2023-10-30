package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jooq.Field;
import org.jooq.impl.DSL;

@EqualsAndHashCode
public class ExistsSqlSelect implements ExplicitSelect {

	private static final String EXISTS_SUFFIX = "_exists";

	@Getter
	private final SqlSelectId sqlSelectId;
	private final String alias;

	public ExistsSqlSelect(ExistsSelect existsSelect, String alias) {
		this.sqlSelectId = SqlSelectId.fromSelect(existsSelect);
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
	public List<String> columnNames() {
		return List.of(alias);
	}

}
