package com.bakdata.conquery.sql.conversion.model.filter;

import lombok.Value;
import org.jooq.Condition;

@Value
public class WhereConditionWrapper implements WhereCondition {

	Condition condition;
	ConditionType type;

	@Override
	public Condition filterCondition() {
		return condition;
	}

	@Override
	public ConditionType type() {
		return type;
	}

}
