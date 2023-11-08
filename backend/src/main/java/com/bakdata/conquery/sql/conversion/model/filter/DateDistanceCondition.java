package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.common.Range;
import org.jooq.Field;

public class DateDistanceCondition extends RangeCondition {

	public DateDistanceCondition(Field<Integer> column, Range.LongRange range) {
		super(column, range);
	}

	@Override
	public FilterType type() {
		return FilterType.EVENT;
	}

}
