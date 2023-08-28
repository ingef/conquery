package com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter;

import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterConditionWrapper;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterType;
import lombok.experimental.UtilityClass;
import org.jooq.Condition;
import org.jooq.Field;

@UtilityClass
public class ConditionUtil {

	public Condition rangeCondition(final Field<?> column, final IRange<?, ?> range) {
		Field<Object> col = (Field<Object>) column;
		Optional<Condition> greaterOrEqualCondition = Optional.ofNullable(range.getMin()).map(col::greaterOrEqual);
		Optional<Condition> lessOrEqualCondition = Optional.ofNullable(range.getMax()).map(col::lessOrEqual);
		return Stream.concat(greaterOrEqualCondition.stream(), lessOrEqualCondition.stream())
					 .reduce(Condition::and)
					 .orElseThrow(() -> new IllegalArgumentException("Missing min or max value for real range filter."));
	}

	/**
	 * Use if you want to wrap a {@link Condition} without implementing a {@link FilterCondition} in an own class.
	 *
	 * @return A {@link FilterCondition} instance encapsulating the provided condition and type.
	 */
	public FilterCondition wrap(final Condition condition, final FilterType type) {
		return new FilterConditionWrapper(condition, type);
	}

}
