package com.bakdata.conquery.sql.translate.ir;

import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.not;
import static org.jooq.impl.DSL.val;

import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.models.common.InclusiveRange;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import lombok.experimental.UtilityClass;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/** Reusable predicates for resolved SQL operations. */
@UtilityClass
public final class Conditions {

	public static Condition dateRestriction(ColumnDateRange dateRestriction, ColumnDateRange validityDate) {
		return dateRestriction.getStart().lessThan(validityDate.getEnd())
				.and(dateRestriction.getEnd().greaterThan(validityDate.getStart()));
	}

	public static Condition flags(List<Field<Boolean>> flagFields) {
		return flagFields.stream()
				.map(DSL::condition)
				.map(Field::isTrue)
				.reduce(Condition::or)
				.orElseThrow(() -> new IllegalArgumentException("Can't construct a flag condition with an empty field list."));
	}

	public static <T extends Comparable<? super T>> Condition inclusiveRange(
			Field<T> field,
			InclusiveRange<T> range
	) {
		Condition condition = noCondition();
		if (range.minimum().isPresent()) {
			condition = condition.and(field.greaterOrEqual(DSL.inline(range.minimum().orElseThrow())));
		}
		if (range.maximum().isPresent()) {
			condition = condition.and(field.lessOrEqual(DSL.inline(range.maximum().orElseThrow())));
		}
		return condition;
	}

	public static Condition stringValues(Field<String> column, String[] values) {
		final String[] nonEmptyValues = Arrays.stream(values)
				.filter(value -> !isNullOrEmpty(value))
				.toArray(String[]::new);
		final Condition valueCondition = switch (nonEmptyValues.length) {
			case 0 -> noCondition();
			case 1 -> column.eq(val(nonEmptyValues[0]));
			default -> column.in(nonEmptyValues);
		};
		final Condition emptyCondition = Arrays.stream(values).anyMatch(Conditions::isNullOrEmpty)
				? column.isNull()
				: noCondition();
		return valueCondition.or(emptyCondition);
	}

	public static Condition negatedStringValues(Field<String> column, String[] values) {
		return not(stringValues(column, values)).or(column.isNull());
	}

	private static boolean isNullOrEmpty(String value) {
		return value == null || value.isEmpty();
	}
}
