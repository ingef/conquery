package com.bakdata.conquery.sql.conversion.model.filter;

import lombok.Value;
import org.jooq.Condition;

@Value
public class FilterConditionWrapper implements FilterCondition {

	Condition condition;
	FilterType type;

	@Override
	public Condition filterCondition() {
		return condition;
	}

	@Override
	public FilterType type() {
		return type;
	}

}
