package com.bakdata.conquery.models.query.filter.event.number;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class MoneyFilterNode extends NumberFilterNode<Range.LongRange> {

	public MoneyFilterNode(Column column, Range.LongRange filterValue) {
		super(column, filterValue);
	}

	@Override
	public MoneyFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new MoneyFilterNode(getColumn(), filterValue);
	}

	@Override
	public boolean contains(Block block, int event) {
		return getFilterValue().contains(block.getMoney(event, getColumn()));
	}
}
