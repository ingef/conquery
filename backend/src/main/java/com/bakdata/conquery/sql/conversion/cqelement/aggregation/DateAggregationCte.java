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
		context = context.qualify(dateAggregationTables.getPredecessor(cteStep));

		QueryStep.QueryStepBuilder builder = this.convertStep(context);

		if (cteStep != DateAggregationCteStep.NODE_NO_OVERLAP) {
			builder = builder.cteName(dateAggregationTables.cteName(cteStep))
							 .predecessors(List.of(previous));
		}
		if (cteStep != DateAggregationCteStep.INVERT && cteStep != DateAggregationCteStep.NODE_NO_OVERLAP) {
			builder = builder.fromTable(QueryStep.toTableLike(dateAggregationTables.getPredecessor(cteStep)));
		}

		return builder.build();
	}

	protected abstract QueryStep.QueryStepBuilder convertStep(DateAggregationContext context);

	public abstract DateAggregationCteStep getCteStep();

}
