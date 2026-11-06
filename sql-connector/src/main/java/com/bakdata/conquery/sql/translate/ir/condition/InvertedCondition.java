package com.bakdata.conquery.sql.translate.ir.condition;

import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.impl.DSL;

/** Logical negation of another translate condition. */
@RequiredArgsConstructor
public class InvertedCondition implements WhereCondition {

	private final WhereCondition filterCondition;

	@Override
	public Condition condition() {
		return DSL.not(filterCondition.condition());
	}

	@Override
	public WhereCondition negate() {
		return filterCondition;
	}
}
