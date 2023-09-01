package com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter;

import java.util.Arrays;
import java.util.Objects;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterType;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Name;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
public class MultiSelectCondition implements FilterCondition {

	private final Name column;
	private final String[] values;
	private final SqlFunctionProvider functionProvider;

	@Override
	public FilterCondition negate() {
		// we want all entries that don't satisfy a condition - because in SQL a comparison with NULL equals UNKNOWN and not FALSE,
		// we need to check if the entry is NULL or does not fulfil the condition
		Condition valueIsNull = DSL.field(column).isNull();
		Condition notOrNull = DSL.not(filterCondition()).or(valueIsNull);
		return ConditionUtil.wrap(notOrNull, this.type());
	}

	@Override
	public Condition filterCondition() {

		// Note here that empty strings are used here as null/missing-value handles.
		// So if values contain "" or null, we want to explicitly filter for empty/NULL entries as well.
		String[] valuesWithoutNull = Arrays.stream(values)
										   .filter(Objects::nonNull)
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
