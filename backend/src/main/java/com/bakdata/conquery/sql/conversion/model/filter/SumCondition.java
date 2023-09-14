package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.common.IRange;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;

@RequiredArgsConstructor
public class SumCondition implements FilterCondition {

	private final Field<? extends Number> sumColumn;
	private final IRange<? extends Number, ?> range;

	@Override
	public Condition filterCondition() {
		return ConditionUtil.rangeCondition(sumColumn, range);
	}

	@Override
	public FilterType type() {
		return FilterType.GROUP;
	}

}
