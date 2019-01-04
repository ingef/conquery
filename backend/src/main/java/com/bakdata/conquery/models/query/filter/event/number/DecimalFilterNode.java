package com.bakdata.conquery.models.query.filter.event.number;

import java.math.BigDecimal;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class DecimalFilterNode extends NumberFilterNode<Range<BigDecimal>> {

	public DecimalFilterNode(SingleColumnFilter filter, FilterValue<Range<BigDecimal>> filterValue) {
		super(filter, filterValue);
	}

	@Override
	public DecimalFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new DecimalFilterNode(filter, filterValue);
	}

	@Override
	public boolean contains(Block block, int event) {
		return getRange().contains(block.getDecimal(event, filter.getColumn()));
	}
}
