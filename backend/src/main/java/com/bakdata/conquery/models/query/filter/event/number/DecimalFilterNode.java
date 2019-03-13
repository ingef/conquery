package com.bakdata.conquery.models.query.filter.event.number;

import java.math.BigDecimal;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;


public class DecimalFilterNode extends NumberFilterNode<Range<BigDecimal>> {

	public DecimalFilterNode(Column column, Range<BigDecimal> filterValue) {
		super(column, filterValue);
	}

	@Override
	public DecimalFilterNode doClone(CloneContext ctx) {
		return new DecimalFilterNode(getColumn(), filterValue);
	}

	@Override
	public boolean contains(Block block, int event) {
		return getFilterValue().contains(block.getDecimal(event, getColumn()));
	}
}
