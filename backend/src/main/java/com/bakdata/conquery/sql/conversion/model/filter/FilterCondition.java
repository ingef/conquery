package com.bakdata.conquery.sql.conversion.model.filter;

import org.jooq.Condition;

public interface FilterCondition {

	Condition filterCondition();

	FilterType type();

	default FilterCondition negate() {
		return new InvertedCondition(this);
	}

}
