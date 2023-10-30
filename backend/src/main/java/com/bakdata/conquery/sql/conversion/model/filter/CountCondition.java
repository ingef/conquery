package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.common.IRange;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;

@RequiredArgsConstructor
public class CountCondition implements FilterCondition {

	private final Field<Integer> countColumn;
	private final IRange<? extends Number, ?> range;

	@Override
	public Condition filterCondition() {
		return ConditionUtil.rangeCondition(countColumn, range);
	}

	@Override
	public FilterType type() {
		return FilterType.GROUP;
	}

}
