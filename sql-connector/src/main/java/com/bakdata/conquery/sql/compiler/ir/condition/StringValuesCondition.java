package com.bakdata.conquery.sql.compiler.ir.condition;

import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.not;
import static org.jooq.impl.DSL.val;

import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;

/** Matches a string column against selected values, treating null and empty selections as an empty database value. */
@RequiredArgsConstructor
public class StringValuesCondition implements WhereCondition {

	private final Field<String> column;
	private final String[] values;

	@Override
	public WhereCondition negate() {
		// SQL comparisons with NULL yield UNKNOWN, so explicitly retain NULL rows when negating a value condition.
		Condition notOrNull = not(condition()).or(column.isNull());
		return new ConditionWrappingWhereCondition(notOrNull);
	}

	@Override
	public Condition condition() {
		String[] nonEmptyValues = Arrays.stream(values)
				.filter(value -> !isNullOrEmpty(value))
				.toArray(String[]::new);

		Condition valueCondition = switch (nonEmptyValues.length) {
			case 0 -> noCondition();
			case 1 -> column.eq(val(nonEmptyValues[0]));
			default -> column.in(nonEmptyValues);
		};

		Condition emptyCondition = Arrays.stream(values).anyMatch(StringValuesCondition::isNullOrEmpty)
				? column.isNull()
				: noCondition();

		return valueCondition.or(emptyCondition);
	}

	private static boolean isNullOrEmpty(String value) {
		return value == null || value.isEmpty();
	}
}
