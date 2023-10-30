package com.bakdata.conquery.sql.conversion.dialect;

import java.util.List;

import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.DateAggregationDates;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;

public interface SqlDateAggregator {

	/**
	 * MERGE or INTERSECT the validity dates of the given {@link QueryStep}s based on the given {@link DateAggregationAction}.
	 *
	 * @param carryThroughSelects The selects that should be carried through the date aggregation process. They remain unchanged.
	 */
	QueryStep apply(
			QueryStep joinedStep,
			List<ExplicitSelect> carryThroughSelects,
			DateAggregationDates dateAggregationDates,
			DateAggregationAction dateAggregationAction,
			NameGenerator nameGenerator
	);

	/**
	 * Inverts the validity date of the given base step.
	 */
	public QueryStep invertAggregatedIntervals(QueryStep baseStep, NameGenerator nameGenerator);

}
