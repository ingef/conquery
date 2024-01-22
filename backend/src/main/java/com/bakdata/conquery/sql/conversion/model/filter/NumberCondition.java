package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.common.IRange;
import org.jooq.Field;

public class NumberCondition extends RangeCondition {

	public NumberCondition(Field<? extends Number> column, IRange<? extends Number, ?> range) {
		super(column, range);
	}

	@Override
	public ConditionType type() {
		return ConditionType.EVENT;
	}

}
