package com.bakdata.conquery.sql.conversion.model.filter;

import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
public class InvertedCondition implements WhereCondition {

	private final WhereCondition filterCondition;

	@Override
	public Condition filterCondition() {
		return DSL.not(filterCondition.filterCondition());
	}

	@Override
	public ConditionType type() {
		return filterCondition.type();
	}

	@Override
	public WhereCondition negate() {
		return filterCondition;
	}

}
