package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

public class DurationSumAggregatorNode extends SingleColumnAggregator<Long> {

	private CDateSet set = CDateSet.create();
	private CDateSet dateRestriction;

	public DurationSumAggregatorNode(Column column) {
		super(column);
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		//otherwise the result would be something weird
		if(block.getAsDateRange(event, getColumn()).isOpen()) {
			return;
		}
		
		CDateSet range = CDateSet.create();
		range.add(block.getAsDateRange(event, getColumn()));
		
		range.retainAll(dateRestriction);

		set.addAll(range);
	}

	@Override
	public DurationSumAggregatorNode clone() {
		return new DurationSumAggregatorNode(getColumn());
	}

	@Override
	public Long getAggregationResult() {
		return set.countDays();
	}
}
