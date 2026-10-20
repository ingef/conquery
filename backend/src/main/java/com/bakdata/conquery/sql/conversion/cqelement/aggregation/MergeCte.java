package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import org.jooq.Condition;
import org.jooq.Field;

class MergeCte extends DateAggregationCte {

	@Override
	protected QueryStep.QueryStepBuilder convertStep(DateAggregationContext noOverlapContext, String noOverlapTable) {

		String joinedTable = noOverlapContext.getDateAggregationTables().getRootTable();
		DateAggregationContext joinedContext = noOverlapContext.qualify(joinedTable);

		QueryStep overlapStep = createOverlapStep(joinedContext, joinedTable);
		List<QueryStep> noOverlapBranches = noOverlapContext.getDateAggregationDates()
																  .getValidityDates()
																  .stream()
																  .map(validityDate -> createNoOverlapBranch(validityDate, noOverlapContext, noOverlapTable))
																  .toList();

		// Only no_overlap is a QueryStep predecessor. It emits joined transitively, while the SELECT may safely reference both CTEs.
		return overlapStep.toBuilder()
						  .union(noOverlapBranches);
	}

	private static QueryStep createOverlapStep(DateAggregationContext context, String joinedTable) {

		SqlFunctionProvider functionProvider = context.getFunctionProvider();
		List<Field<Date>> allStarts = context.getDateAggregationDates().allStarts();
		List<Field<Date>> allEnds = context.getDateAggregationDates().allEnds();

		ColumnDateRange overlapValidityDate = ColumnDateRange.of(
				functionProvider.least(allStarts).as(DateAggregationCte.RANGE_START),
				functionProvider.greatest(allEnds).as(DateAggregationCte.RANGE_END)
		).asValidityDateRange(joinedTable);

		Selects overlapSelects = Selects.builder()
										.ids(context.getIds())
										.validityDate(Optional.of(overlapValidityDate))
										.sqlSelects(context.getCarryThroughSelects())
										.build();

		return QueryStep.builder()
						.selects(overlapSelects)
						.fromTable(QueryStep.toTableLike(joinedTable))
						.conditions(List.of(overlapCondition(allStarts, allEnds, functionProvider)))
						.build();
	}

	private static QueryStep createNoOverlapBranch(
			ColumnDateRange validityDate,
			DateAggregationContext context,
			String noOverlapTable
	) {

		Field<Date> start = validityDate.getStart();
		ColumnDateRange selectedValidityDate = ColumnDateRange.of(
				start.as(DateAggregationCte.RANGE_START),
				validityDate.getEnd().as(DateAggregationCte.RANGE_END)
		);

		Selects selects = Selects.builder()
								 .ids(context.getIds())
								 .validityDate(Optional.of(selectedValidityDate))
								 .sqlSelects(context.getCarryThroughSelects())
								 .build();

		return QueryStep.builder()
						.selects(selects)
						.fromTable(QueryStep.toTableLike(noOverlapTable))
						.conditions(List.of(start.isNotNull()))
						.build();
	}

	static Condition overlapCondition(List<Field<Date>> allStarts, List<Field<Date>> allEnds, SqlFunctionProvider functionProvider) {
		Condition startBeforeEnd = functionProvider.greatest(allStarts).lessThan(functionProvider.least(allEnds));
		Condition allStartsNotNull = allStarts.stream()
												 .map(Field::isNotNull)
												 .reduce(Condition::and)
												 .orElseThrow();
		return allStartsNotNull.and(startBeforeEnd);
	}

	@Override
	public DateAggregationCteStep getCteStep() {
		return DateAggregationCteStep.MERGE;
	}

}
