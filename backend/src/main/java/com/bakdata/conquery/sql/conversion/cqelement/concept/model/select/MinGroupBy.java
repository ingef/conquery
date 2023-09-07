package com.bakdata.conquery.sql.conversion.cqelement.concept.model.select;

import java.math.BigDecimal;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode
public class MinGroupBy implements ConquerySelect {

	private final Field<?> minColumn;

	@Override
	public Field<?> select() {
		return DSL.min(minColumn)
				  .as(minColumn.getName());
	}

	@Override
	public Field<?> alias() {
		return DSL.field(minColumn.getName(), BigDecimal.class);
	}

}
