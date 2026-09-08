package com.bakdata.conquery.sql.compiler.ir.condition;

import com.bakdata.conquery.models.common.InclusiveRange;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/** Matches values within optional inclusive lower and upper bounds. */
@RequiredArgsConstructor
public class InclusiveRangeCondition<T extends Comparable<? super T>> implements WhereCondition {

	private final Field<T> field;
	private final InclusiveRange<T> range;

	@Override
	public Condition condition() {
		Condition condition = DSL.noCondition();

		if (range.minimum().isPresent()) {
			condition = condition.and(field.greaterOrEqual(DSL.inline(range.minimum().get())));
		}

		if (range.maximum().isPresent()) {
			condition = condition.and(field.lessOrEqual(DSL.inline(range.maximum().get())));
		}

		return condition;
	}
}
