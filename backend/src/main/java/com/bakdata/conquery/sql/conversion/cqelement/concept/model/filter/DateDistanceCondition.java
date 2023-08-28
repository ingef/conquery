package com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterType;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;

@RequiredArgsConstructor
public class DateDistanceCondition implements FilterCondition {

	private final Field<Integer> distanceColumn;
	private final Range.LongRange range;

	@Override
	public Condition filterCondition() {
		return ConditionUtil.rangeCondition(distanceColumn.coerce(Long.class), range);
	}

	@Override
	public FilterType type() {
		return FilterType.EVENT;
	}

}
