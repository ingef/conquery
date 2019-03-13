package com.bakdata.conquery.models.query.filter.event.number;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

public class IntegerFilterNode extends NumberFilterNode<Range.LongRange> {


	public IntegerFilterNode(SingleColumnFilter filter, FilterValue<Range.LongRange> filterValue) {
		super(filter, filterValue);
	}

	@Override
	public IntegerFilterNode doClone(CloneContext ctx) {
		return new IntegerFilterNode(filter, filterValue);
	}

	@Override
	public boolean contains(Block block, int event) {
		return getRange().contains(block.getInteger(event, filter.getColumn()));
	}
}
