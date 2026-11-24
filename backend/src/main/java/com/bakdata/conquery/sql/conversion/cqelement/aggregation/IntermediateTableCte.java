
package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;

/**
 * Collects joined rows whose validity-date ranges do not overlap into the shared no_overlap CTE.
 * <p>
 * This intermediate step is only necessary for MERGE: {@link MergeCte} reads it once and emits each non-null input range in a separate
 * UNION ALL branch before interval packing. Overlapping rows are handled directly by {@link MergeCte}.
 * INTERSECT calculates its result in the joined step and therefore does not use this CTE.
 */
class IntermediateTableCte extends DateAggregationCte {

	@Override
	protected QueryStep.QueryStepBuilder convertStep(DateAggregationContext context, String predecessor) {

		List<SqlSelect> intermediateTableSelects = Stream.of(
													  context.getDateAggregationDates().allStartsAndEnds(),
													  context.getCarryThroughSelects()
											  )
											  .flatMap(Collection::stream)
											  .collect(Collectors.toList());
		Selects selects = Selects.builder()
								 .ids(context.getIds())
								 .sqlSelects(intermediateTableSelects)
								 .build();

		DateAggregationDates dateAggregationDates = context.getDateAggregationDates();
		Condition intermediateTableCondition = DateAggregationConditions.overlapCondition(dateAggregationDates, context.getFunctionProvider()).not();

		return QueryStep.builder()
						.selects(selects)
						.fromTable(QueryStep.toTableLike(predecessor))
						.conditions(List.of(intermediateTableCondition));
	}

	@Override
	public DateAggregationCteStep getCteStep() {
		return DateAggregationCteStep.INTERMEDIATE_TABLE;
	}

}
