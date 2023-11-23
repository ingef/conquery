package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;

@RequiredArgsConstructor
class MergeAggregateAction implements SqlAggregationAction {

	private final QueryStep joinedStep;

	@Override
	public DateAggregationTables<MergeCteStep> tableNames() {
		return MergeCteStep.tableNames(this.joinedStep);
	}

	@Override
	public List<DateAggregationCte> dateAggregationCtes() {
		return MergeCteStep.requiredSteps();
	}

	@Override
	public ColumnDateRange getOverlapValidityDate(DateAggregationDates dateAggregationDates, SqlFunctionProvider functionProvider) {

		Field<Date> rangeStart = functionProvider.least(dateAggregationDates.allStarts());
		Field<Date> rangeEnd = functionProvider.greatest(dateAggregationDates.allEnds());

		return ColumnDateRange.of(
				rangeStart.as(DateAggregationCte.RANGE_START),
				rangeEnd.as(DateAggregationCte.RANGE_END)
		);
	}

	@Override
	public List<SqlSelect> getIntermediateTableSelects(DateAggregationDates dateAggregationDates, List<SqlSelect> carryThroughSelects) {
		return Stream.of(dateAggregationDates.allStartsAndEnds(), carryThroughSelects)
					 .flatMap(Collection::stream)
					 .collect(Collectors.toList());
	}

	@Override
	public List<QueryStep> getNoOverlapSelects(DateAggregationContext dateAggregationContext) {
		return dateAggregationContext.getSteps(MergeCteStep.NODE_NO_OVERLAP);
	}

	@Override
	public QueryStep getOverlapStep(DateAggregationContext dateAggregationContext) {
		return dateAggregationContext.getStep(MergeCteStep.OVERLAP);
	}

	@Override
	public boolean requiresIntervalPackingAfterwards() {
		return true;
	}

}
