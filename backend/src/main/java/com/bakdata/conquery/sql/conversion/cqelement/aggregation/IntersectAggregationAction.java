package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.List;

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
	public DateAggregationTables tableNames(NameGenerator nameGenerator) {
		return IntersectStep.tableNames(this.joinedStep, nameGenerator);
	}

	@Override
	public List<DateAggregationCte> dateAggregationCtes() {
		return IntersectStep.requiredSteps();
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
	public List<SqlSelect> getIntermediateTableSelects(DateAggregationDates dateAggregationDates) {
		return List.of(emptyRange(DateAggregationCte.RANGE_START), emptyRange(DateAggregationCte.RANGE_END));
	}

	@Override
	public List<QueryStep> getNoOverlapSelects(DateAggregationContext dateAggregationContext) {
		return List.of(dateAggregationContext.getStep(IntersectStep.INTERMEDIATE_TABLE));
	}

	@Override
	public QueryStep getOverlapStep(DateAggregationContext dateAggregationContext) {
		return dateAggregationContext.getStep(IntersectStep.OVERLAP);
	}

	private static SqlSelect emptyRange(String alias) {
		return new FieldWrapper(DSL.inline(null, Date.class).as(alias));
	}

}
