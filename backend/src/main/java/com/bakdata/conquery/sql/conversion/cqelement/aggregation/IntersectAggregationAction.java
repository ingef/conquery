package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
class IntersectAggregationAction implements SqlAggregationAction {

	private final QueryStep joinedStep;

	@Override
	public DateAggregationTables<IntersectCteStep> tableNames(NameGenerator nameGenerator) {
		return IntersectCteStep.createTableNames(this.joinedStep, nameGenerator);
	}

	@Override
	public List<DateAggregationCte> dateAggregationCtes() {
		return IntersectCteStep.requiredSteps();
	}

	@Override
	public ColumnDateRange getOverlapValidityDate(DateAggregationDates dateAggregationDates, SqlFunctionProvider functionProvider) {

		Field<Date> rangeStart = functionProvider.greatest(dateAggregationDates.allStarts());
		Field<Date> rangeEnd = functionProvider.least(dateAggregationDates.allEnds());

		return ColumnDateRange.of(
				rangeStart.as(DateAggregationCte.RANGE_START),
				rangeEnd.as(DateAggregationCte.RANGE_END)
		);
	}

	@Override
	public List<SqlSelect> getIntermediateTableSelects(DateAggregationDates dateAggregationDates, List<SqlSelect> carryThroughSelects) {

		List<FieldWrapper<?>> nulledRangeStartAndEnd =
				Stream.of(
							  DSL.inline(null, Date.class).as(DateAggregationCte.RANGE_START),
							  DSL.inline(null, Date.class).as(DateAggregationCte.RANGE_END)
					  )
					  .map(FieldWrapper::new)
					  .collect(Collectors.toList());

		return Stream.of(nulledRangeStartAndEnd, carryThroughSelects)
					 .flatMap(Collection::stream)
					 .collect(Collectors.toList());
	}

	@Override
	public List<QueryStep> getNoOverlapSelects(DateAggregationContext dateAggregationContext) {
		return List.of(dateAggregationContext.getStep(IntersectCteStep.INTERMEDIATE_TABLE));
	}

	@Override
	public QueryStep getOverlapStep(DateAggregationContext dateAggregationContext) {
		return dateAggregationContext.getStep(IntersectCteStep.OVERLAP);
	}

	@Override
	public boolean requiresIntervalPackingAfterwards() {
		return false;
	}

}
