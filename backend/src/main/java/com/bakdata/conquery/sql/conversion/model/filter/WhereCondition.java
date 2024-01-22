package com.bakdata.conquery.sql.conversion.model.filter;

import org.jooq.Condition;

public interface WhereCondition {

	Condition condition();

	ConditionType type();

	default WhereCondition negate() {
		return new InvertedCondition(this);
	}

}
