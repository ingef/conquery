package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.List;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.experimental.UtilityClass;
import org.jooq.Condition;
import org.jooq.Field;

/**
 * Creates predicates that classify the validity-date ranges participating in a date aggregation.
 * <p>
 * The corresponding non-overlap predicate is obtained with {@link Condition#not()}, which deliberately retains {@code UNKNOWN} for nullable end expressions.
 */
@UtilityClass
final class DateAggregationConditions {

	static Condition overlapCondition(DateAggregationDates dateAggregationDates, SqlFunctionProvider functionProvider) {
		List<Field<Date>> allStarts = dateAggregationDates.allStarts();
		List<Field<Date>> allEnds = dateAggregationDates.allEnds();

		Condition startBeforeEnd = functionProvider.greatest(allStarts).lessThan(functionProvider.least(allEnds));
		Condition allStartsNotNull = allStarts.stream()
				.map(Field::isNotNull)
				.reduce(Condition::and)
				.orElseThrow();

		return allStartsNotNull.and(startBeforeEnd);
	}

}
