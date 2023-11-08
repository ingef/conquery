package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.common.IRange;
import org.jooq.Field;

public class SumCondition extends RangeCondition {

	public SumCondition(Field<? extends Number> column, IRange<? extends Number, ?> range) {
		super(column, range);
	}

	@Override
	public FilterType type() {
		return FilterType.GROUP;
	}

}
