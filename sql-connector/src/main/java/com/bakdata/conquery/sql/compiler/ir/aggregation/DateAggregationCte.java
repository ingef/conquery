package com.bakdata.conquery.sql.compiler.ir.aggregation;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;

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

		return this.convertStep(context, predecessor, previous);
	}

	protected abstract QueryStep convertStep(DateAggregationContext context, String predecessor, QueryStep previous);

	public abstract DateAggregationCteStep getCteStep();

}
