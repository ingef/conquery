package com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterType;
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
