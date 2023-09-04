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

	@Override
	public Field<BigDecimal> select() {
		return DSL.sum(DSL.field(DSL.name(columnToSum.getName()), columnToSum.getType()))
				  .as(columnToSum.getName());
	}

	@Override
	public Field<BigDecimal> alias() {
		return DSL.field(columnToSum.getName(), BigDecimal.class);
	}

}
