package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;

/**
 * Base class for a CTE that is part of the date aggregation process.
 */
abstract class DateAggregationCte {

	public static final String RANGE_START = "RANGE_START";
	public static final String RANGE_END = "RANGE_END";

	public QueryStep convert(DateAggregationContext context, QueryStep previous) {

		DateAggregationCteStep cteStep = getCteStep();
		SqlTables dateAggregationTables = context.getDateAggregationTables();

		// this way all selects are already qualified, and we don't need to care for that in the respective steps
		String predecessor = dateAggregationTables.getPredecessor(cteStep);
		context = context.qualify(predecessor);

		QueryStep.QueryStepBuilder builder = this.convertStep(context, predecessor);
		return builder.cteName(dateAggregationTables.cteName(cteStep))
					  .predecessors(List.of(previous))
					  .build();
	}

	protected abstract QueryStep.QueryStepBuilder convertStep(DateAggregationContext context, String predecessor);

	public abstract DateAggregationCteStep getCteStep();

}
