package com.bakdata.conquery.models.query.filter.event.number;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;


public class IntegerFilterNode extends NumberFilterNode<Range.LongRange> {

	public IntegerFilterNode(Column column, Range.LongRange filterValue) {
		super(column, filterValue);
	}

	@Override
	public IntegerFilterNode doClone(CloneContext ctx) {
		return new IntegerFilterNode(getColumn(), filterValue);
	}

	@Override
	public boolean contains(Block block, int event) {
		return getFilterValue().contains(block.getInteger(event, getColumn()));
	}
}
