package com.bakdata.conquery.sql.conversion.model.filter;

import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.IRange;
import lombok.experimental.UtilityClass;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

@UtilityClass
public class ConditionUtil {

	public <T extends Comparable<?>> Condition rangeCondition(final Field<T> column, final IRange<T, ?> range) {
		Condition condition = DSL.noCondition();

		if (range.hasLowerBound()){
			condition = condition.and(column.greaterOrEqual(DSL.inline(range.getMin())));
		}

		if (range.hasUpperBound()){
			condition = condition.and(column.lessOrEqual(DSL.inline(range.getMax())));
		}


		return condition;
	}

	/**
	 * Use if you want to wrap a {@link Condition} without implementing a {@link WhereCondition} in an own class.
	 *
	 * @return A {@link WhereCondition} instance encapsulating the provided condition and type.
	 */
	public WhereCondition wrap(final Condition condition) {
		return new ConditionWrappingWhereCondition(condition);
	}

}
