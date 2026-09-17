package com.bakdata.conquery.sql.conversion.model.filter;

import java.util.function.BiFunction;

import org.jooq.Condition;

public interface WhereCondition {

	Condition condition();

	default WhereCondition negate() {
		return new InvertedCondition(this);
	}

	default WhereCondition and(WhereCondition whereCondition) {
		return combineConditions(whereCondition, Condition::and);
	}

	default WhereCondition or(WhereCondition whereCondition) {
		return combineConditions(whereCondition, Condition::or);
	}

	private WhereCondition combineConditions(WhereCondition whereCondition, BiFunction<Condition, Condition, Condition> operation) {
		Condition combinedCondition = operation.apply(this.condition(), whereCondition.condition());
		return new ConditionWrappingWhereCondition(combinedCondition);
	}

}
