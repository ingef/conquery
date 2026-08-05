package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.common.IRange;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;

@RequiredArgsConstructor
abstract class RangeCondition<T extends Comparable<?>> implements WhereCondition {

	private final Field<T> column;
	private final IRange<T,?> range;

	@Override
	public Condition condition() {
		return ConditionUtil.rangeCondition(column, range);
	}

}
