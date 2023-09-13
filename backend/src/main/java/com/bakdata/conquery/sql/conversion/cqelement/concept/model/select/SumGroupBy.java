package com.bakdata.conquery.sql.conversion.cqelement.concept.model.select;

import java.math.BigDecimal;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode
public class SumGroupBy implements ConquerySelect {

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
