package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Getter;

/**
 * Aggregator, counting the number of days present.
 */
public class DurationSumAggregator implements Aggregator<Long> {

	private CDateSet set = CDateSet.create();
	private CDateSet dateRestriction;

	@Getter
	private Column column;

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		dateRestriction = ctx.getDateRestriction();
		column = ctx.getValidityDateColumn();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		final CDateRange value = bucket.getAsDateRange(event, getColumn());

		CDateSet range = CDateSet.create();
		range.add(value);

		range.retainAll(dateRestriction);

		//otherwise the result would be something weird
		if(range.isOpen()){
			return;
		}

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
