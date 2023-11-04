package com.bakdata.conquery.sql.conversion.model.select;

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
	private final String label;

	public ExistsSqlSelect(ExistsSelect existsSelect, String label) {
		this.sqlSelectId = SqlSelectId.fromSelect(existsSelect);
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
	public String columnName() {
		return label;
	}

}
