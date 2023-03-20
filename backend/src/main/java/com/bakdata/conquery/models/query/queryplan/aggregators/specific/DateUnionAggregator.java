package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

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
 * Aggregator, listing all days present.
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class DateUnionAggregator extends SingleColumnAggregator<CDateSet> {

	private CDateSet set = CDateSet.createEmpty();
	private CDateSet dateRestriction;

	private int realUpperBound;

	public DateUnionAggregator(Column column) {
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
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		final CDateRange value = bucket.getAsDateRange(event, getColumn());

		set.maskedAdd(value, dateRestriction, realUpperBound);
	}

	@Override
	public CDateSet createAggregationResult() {
		return CDateSet.create(set.asRanges());
	}

	@Override
	public ResultType getResultType() {
		return new ResultType.ListT(ResultType.DateRangeT.INSTANCE);
	}
}
