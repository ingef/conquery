package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.common.IRange;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;

@RequiredArgsConstructor
public class NumberCondition implements FilterCondition {

	private final Field<? extends Number> numberColumn;
	private final IRange<? extends Number, ?> range;

	@Override
	public Condition filterCondition() {
		return ConditionUtil.rangeCondition(numberColumn, range);
	}

	@Override
	public FilterType type() {
		return FilterType.EVENT;
	}

}
