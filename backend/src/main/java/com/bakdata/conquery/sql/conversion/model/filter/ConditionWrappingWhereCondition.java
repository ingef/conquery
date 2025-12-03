package com.bakdata.conquery.sql.conversion.model.filter;

import lombok.Value;
import org.jooq.Condition;

@Value
public class ConditionWrappingWhereCondition implements WhereCondition {

	Condition condition;

	@Override
	public Condition condition() {
		return condition;
	}

}
