package com.bakdata.conquery.models.query.aggregators.filter.number;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.specific.NumberFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class MoneyFilterNode extends NumberFilterNode<Long> {

	public MoneyFilterNode(NumberFilter filter, FilterValue<Range.LongRange> filterValue) {
		super(filter, filterValue);
	}

	@Override
	protected Long getValue(Block block, int event, Column column) {
		return block.getMoney(event, column);
	}

	@Override
	public MoneyFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new MoneyFilterNode(filter, (FilterValue<Range.LongRange>) filterValue);
	}
}
