package com.bakdata.conquery.models.query.filter.event.number;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

public class RealFilterNode extends NumberFilterNode<Range.DoubleRange> {

	public RealFilterNode(SingleColumnFilter filter, FilterValue<Range.DoubleRange> filterValue) {
		super(filter, filterValue);
	}

	@Override
	public RealFilterNode doClone(CloneContext ctx) {
		return new RealFilterNode(filter, filterValue);
	}

	@Override
	public boolean contains(Block block, int event) {
		return getRange().contains(block.getReal(event, filter.getColumn()));
	}
}
