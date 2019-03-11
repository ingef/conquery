package com.bakdata.conquery.models.query.filter.event.number;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class MoneyFilterNode extends NumberFilterNode<Range.LongRange> {


	public MoneyFilterNode(SingleColumnFilter filter, Range.LongRange filterValue) {
		super(filter, filterValue);
	}

	@Override
	public MoneyFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new MoneyFilterNode(filter, filterValue);
	}

	@Override
	public boolean contains(Block block, int event) {
		return getFilterValue().contains(block.getMoney(event, filter.getColumn()));
	}
}
