package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import java.util.Collection;

/**
 * Aggregator, listing all days present.
 */
public class DateUnionAggregator extends SingleColumnAggregator<CDateSet> {

	private CDateSet set = CDateSet.create();
	private CDateSet dateRestriction;

	public DateUnionAggregator(Column column) {
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

		CDateRange value = bucket.getAsDateRange(event, getColumn());
		//otherwise the result would be something weird
		if (value.isOpen()) {
			return;
		}

		set.maskedAdd(value, dateRestriction);
	}

	@Override
	public DateUnionAggregator doClone(CloneContext ctx) {
		return new DateUnionAggregator(getColumn());
	}

	@Override
	public CDateSet getAggregationResult() {
		return set;
	}

	@Override
	public ResultType getResultType() {
		return new ResultType.ListT(ResultType.DateRangeT.INSTANCE);
	}
}
