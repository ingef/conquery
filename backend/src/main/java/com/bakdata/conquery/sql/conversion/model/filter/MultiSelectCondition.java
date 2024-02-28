package com.bakdata.conquery.sql.conversion.model.filter;

import java.util.Arrays;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
public class MultiSelectCondition implements WhereCondition {

	private final Field<String> column;
	private final String[] values;
	private final SqlFunctionProvider functionProvider;

	@Override
	public WhereCondition negate() {
		// we want all entries that don't satisfy a condition - because in SQL a comparison with NULL equals UNKNOWN and not FALSE,
		// we need to check if the entry is NULL or does not fulfil the condition
		Condition valueIsNull = column.isNull();
		Condition notOrNull = DSL.not(condition()).or(valueIsNull);
		return ConditionUtil.wrap(notOrNull, this.type());
	}

	@Override
	public Condition condition() {

		// values can contain empty or null Strings
		String[] valuesWithoutNull = Arrays.stream(values)
										   .filter(value -> !Strings.isNullOrEmpty(value))
										   .toArray(String[]::new);
		Condition inCondition = this.functionProvider.in(column, valuesWithoutNull);

		if (valuesWithoutNull.length < values.length) {
			return inCondition.or(DSL.field(column).isNull());
		}
		return inCondition;
	}

	@Override
	public ConditionType type() {
		return ConditionType.EVENT;
	}

}
