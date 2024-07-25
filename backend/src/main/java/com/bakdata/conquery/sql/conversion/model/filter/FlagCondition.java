package com.bakdata.conquery.sql.conversion.model.filter;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
public class FlagCondition implements WhereCondition {

	private final List<Field<Boolean>> flagFields;

	@Override
	public Condition condition() {
		return flagFields.stream()
						 .map(DSL::condition)
						 .map(Field::isTrue)
						 .reduce(Condition::or)
						 .orElseThrow(() -> new IllegalArgumentException("Can't construct a FlagCondition with an empty flag field list."));
	}

	@Override
	public ConditionType type() {
		return ConditionType.EVENT;
	}

}
