package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import lombok.Getter;
import org.jooq.Condition;
import org.jooq.Field;

class NodeNoOverlapCte extends DateAggregationCte {

	@Getter
	private final DateAggregationStep cteStep;
	private int counter = 0; // used to make each no-overlap CTE name unique

	public NodeNoOverlapCte(DateAggregationStep cteStep) {
		this.cteStep = cteStep;
	}

	@Override
	protected QueryStep.QueryStepBuilder convertStep(DateAggregationContext context) {

		// we create a no-overlap node for each query step we need to aggregate
		DateAggregationDates dateAggregationDates = context.getDateAggregationDates();
		Iterator<ColumnDateRange> validityDates = dateAggregationDates.getValidityDates().iterator();
		QueryStep intermediateTableStep = context.getStep(MergeStep.INTERMEDIATE_TABLE);

		// first no-overlap step has intermediate table as predecessor
		QueryStep.QueryStepBuilder noOverlapStep = createNoOverlapStep(validityDates.next(), context, intermediateTableStep);

		// each following step has it's predeceasing no-overlap as predecessor
		while (validityDates.hasNext()) {
			counter++;
			QueryStep predeceasingNoOverlapStep = noOverlapStep.build();
			context.withStep(getCteStep(), predeceasingNoOverlapStep);
			noOverlapStep = createNoOverlapStep(validityDates.next(), context, predeceasingNoOverlapStep);
		}

		return noOverlapStep;
	}

	private QueryStep.QueryStepBuilder createNoOverlapStep(
			ColumnDateRange validityDate,
			DateAggregationContext context,
			QueryStep predecessor
	) {

		DateAggregationTables dateAggregationTables = context.getDateAggregationTables();

		Field<Date> start = validityDate.getStart();
		Field<Date> end = validityDate.getEnd();

		Field<Date> asRangeEnd = end.as(DateAggregationCte.RANGE_END);
		Field<Date> asRangeStart = start.as(DateAggregationCte.RANGE_START);
		String intermediateTableCteName = dateAggregationTables.getFromTableOf(getCteStep());
		Selects nodeNoOverlapSelects = Selects.builder()
											  .primaryColumn(context.getPrimaryColumn())
											  .validityDate(Optional.of(ColumnDateRange.of(asRangeStart, asRangeEnd)))
											  .explicitSelects(context.getCarryThroughSelects())
											  .build();

		Condition startNotNull = start.isNotNull();

		return QueryStep.builder()
						.cteName("%s_%s".formatted(dateAggregationTables.cteName(MergeStep.NODE_NO_OVERLAP), counter))
						.selects(nodeNoOverlapSelects)
						.fromTable(QueryStep.toTableLike(intermediateTableCteName))
						.conditions(List.of(startNotNull))
						.predecessors(List.of(predecessor));
	}

}
