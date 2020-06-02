package com.bakdata.conquery.models.query.queryplan.aggregators.specific.date;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator, counting the number of days present.
 */
public class DurationSumAggregator implements Aggregator<Long> {

	private CDateSet set = CDateSet.create();
	private CDateSet dateRestriction;
	private Column column;

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		dateRestriction = ctx.getDateRestriction();

		column = ctx.getValidityDateColumn();
		if(!column.getType().isDateCompatible()){
			throw new IllegalStateException(String.format("Non date-compatible validityDate-Column[%s]", column));
		}
	}

	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		if (!bucket.has(event, column)) {
			return;
		}

		 CDateRange dateRange = bucket.getAsDateRange(event, column);

		//otherwise the result would be something weird
		if(dateRange.isOpen()) {
			return;
		}

		CDateSet range = CDateSet.create();
		range.add(dateRange);

		range.retainAll(dateRestriction);

		set.addAll(range);
	}

	@Override
	public DurationSumAggregator doClone(CloneContext ctx) {
		return new DurationSumAggregator();
	}

	@Override
	public Long getAggregationResult() {
		return set.isEmpty() ? null : set.countDays();
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
