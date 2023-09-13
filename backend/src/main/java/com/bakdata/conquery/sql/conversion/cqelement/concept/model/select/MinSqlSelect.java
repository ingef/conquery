package com.bakdata.conquery.sql.conversion.cqelement.concept.model.select;

import java.math.BigDecimal;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelect;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode
public class MinSqlSelect implements SqlSelect {

	private final Field<?> minColumn;
	private final String alias;

	@Override
	public Field<?> select() {
		return DSL.min(minColumn)
				  .as(alias);
	}

	@Override
	public Field<?> aliased() {
		return DSL.field(alias, BigDecimal.class);
	}

	@Override
	public String columnName() {
		return minColumn.getName();
	}

}
