package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.common.IRange;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;

@RequiredArgsConstructor
abstract class RangeCondition implements FilterCondition {

	private final Field<?> column;
	private final IRange<?,?> range;

	@Override
	public Condition filterCondition() {
		return ConditionUtil.rangeCondition(column, range);
	}

}
