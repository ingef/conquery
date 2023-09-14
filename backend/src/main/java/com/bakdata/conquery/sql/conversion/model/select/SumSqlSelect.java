package com.bakdata.conquery.sql.conversion.model.select;

import java.math.BigDecimal;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode
public class SumSqlSelect implements SqlSelect {

	private final Field<? extends Number> columnToSum;
	private final String alias;

	@Override
	public Field<BigDecimal> select() {
		return DSL.sum(columnToSum)
				  .as(alias);
	}

	@Override
	public Field<BigDecimal> aliased() {
		return DSL.field(alias, BigDecimal.class);
	}

	@Override
	public String columnName() {
		return columnToSum.getName();
	}

}
