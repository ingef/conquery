package com.bakdata.conquery.sql.compiler.ir.condition;

import lombok.Value;
import org.jooq.Condition;

/** Adapts an arbitrary jOOQ condition to the compiler's composable condition abstraction. */
@Value
public class ConditionWrappingWhereCondition implements WhereCondition {

	Condition condition;

	@Override
	public Condition condition() {
		return condition;
	}
}
