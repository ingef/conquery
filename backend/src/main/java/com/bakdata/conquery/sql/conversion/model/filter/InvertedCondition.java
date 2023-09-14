package com.bakdata.conquery.sql.conversion.model.filter;

import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
public class InvertedCondition implements FilterCondition {

	private final FilterCondition filterCondition;

	@Override
	public Condition filterCondition() {
		return DSL.not(filterCondition.filterCondition());
	}

	@Override
	public FilterType type() {
		return filterCondition.type();
	}

	@Override
	public FilterCondition negate() {
		return filterCondition;
	}

}
