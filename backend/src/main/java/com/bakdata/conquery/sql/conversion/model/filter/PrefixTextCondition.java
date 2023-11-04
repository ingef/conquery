package com.bakdata.conquery.sql.conversion.model.filter;

import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;

@RequiredArgsConstructor
public class PrefixTextCondition implements FilterCondition {

	private final Field<String> prefixTextColumn;
	private final String value;

	@Override
	public Condition filterCondition() {
		return prefixTextColumn.like(value + "%");
	}

	@Override
	public FilterType type() {
		return FilterType.EVENT;
	}

}
