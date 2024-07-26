package com.bakdata.conquery.sql.conversion.model.filter;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
public class FlagCondition implements WhereCondition {

	private final List<Field<Boolean>> includedFlags;
	private final List<Field<Boolean>> excludedFlags;

	@Override
	public Condition condition() {
		Condition excludedFlagsFalseCondition = excludedFlags.stream()
															 .map(DSL::condition)
															 .map(field -> field.isFalse().or(field.isNull()))
															 .reduce(Condition::and)
															 .orElse(DSL.noCondition());
		return includedFlags.stream()
							.map(DSL::condition)
							.map(Field::isTrue)
							.reduce(Condition::or)
							.orElseThrow(() -> new IllegalArgumentException("Can't construct a FlagCondition with empty flag field lists."))
							.and(excludedFlagsFalseCondition);
	}

	@Override
	public ConditionType type() {
		return ConditionType.EVENT;
	}

}
