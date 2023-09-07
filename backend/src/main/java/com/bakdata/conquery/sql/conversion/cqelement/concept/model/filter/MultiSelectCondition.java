package com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter;

import java.util.Arrays;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterType;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
public class MultiSelectCondition implements FilterCondition {

	private final Field<String> column;
	private final String[] values;
	private final SqlFunctionProvider functionProvider;

	@Override
	public FilterCondition negate() {
		// we want all entries that don't satisfy a condition - because in SQL a comparison with NULL equals UNKNOWN and not FALSE,
		// we need to check if the entry is NULL or does not fulfil the condition
		Condition valueIsNull = column.isNull();
		Condition notOrNull = DSL.not(filterCondition()).or(valueIsNull);
		return ConditionUtil.wrap(notOrNull, this.type());
	}

	@Override
	public Condition filterCondition() {

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
	public FilterType type() {
		return FilterType.EVENT;
	}

}
