package com.bakdata.conquery.sql.conversion.dialect;

import java.util.List;

import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.DateAggregationDates;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;

public interface SqlDateAggregator {

	/**
	 * MERGE or INTERSECT the validity dates of the given {@link QueryStep}s based on the given {@link DateAggregationAction}.
	 *
	 * @param carryThroughSelects The selects that should be carried through the date aggregation process. They remain unchanged.
	 */
	QueryStep apply(
			QueryStep joinedStep,
			List<SqlSelect> carryThroughSelects,
			DateAggregationDates dateAggregationDates,
			DateAggregationAction dateAggregationAction
	);

	/**
	 * Inverts the validity date of the given base step.
	 */
	QueryStep invertAggregatedIntervals(QueryStep baseStep);

}
