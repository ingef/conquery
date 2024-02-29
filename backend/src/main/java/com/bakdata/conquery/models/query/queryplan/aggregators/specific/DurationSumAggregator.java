package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.ToString;

/**
 * Aggregator, counting the number of days present.
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class DurationSumAggregator extends SingleColumnAggregator<Long> {

	private CDateSet set = CDateSet.createEmpty();
	private CDateSet dateRestriction;

	private int realUpperBound;

	public DurationSumAggregator(Column column) {
		super(column);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		set.clear();
		realUpperBound = context.getToday();
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public void consumeEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		final CDateRange value = bucket.getAsDateRange(event, getColumn());

		set.maskedAdd(value, dateRestriction, realUpperBound);
	}

	@Override
	public Long createAggregationResult() {
		if (set.isEmpty() || CDate.isNegativeInfinity(set.getMinValue())) {
			return null;
		}
		return set.countDays();
	}

	@Override
	public ResultType getResultType() {
		return ResultType.IntegerT.INSTANCE;
	}
}
