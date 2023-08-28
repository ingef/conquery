package com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterType;
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
	public FilterCondition invert() {
		return filterCondition;
	}

}
