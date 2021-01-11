package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator, counting the number of days present.
 */
public class DurationSumAggregator extends SingleColumnAggregator<Long> {

	private CDateSet set = CDateSet.create();
	private CDateSet dateRestriction;

	public DurationSumAggregator(Column column) {
		super(column);
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		final CDateRange value = bucket.getAsDateRange(event, getColumn());

    if(value.isOpen()) {
      return;
    }
    

		set.maskedAdd(value, dateRestriction);
	}

	@Override
	public DurationSumAggregator doClone(CloneContext ctx) {
		return new DurationSumAggregator(getColumn());
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
