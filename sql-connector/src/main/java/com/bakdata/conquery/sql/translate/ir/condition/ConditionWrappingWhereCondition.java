package com.bakdata.conquery.sql.translate.ir.condition;

import lombok.Value;
import org.jooq.Condition;

/** Adapts an arbitrary jOOQ condition to the translate's composable condition abstraction. */
@Value
public class ConditionWrappingWhereCondition implements WhereCondition {

	Condition condition;

	@Override
	public Condition condition() {
		return condition;
	}
}
