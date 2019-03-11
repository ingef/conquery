package com.bakdata.conquery.models.query.filter.event.number;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

public abstract class NumberFilterNode<RANGE extends IRange<?, ?>> extends FilterNode<RANGE, SingleColumnFilter<RANGE>> {

	private boolean hit;

	public NumberFilterNode(SingleColumnFilter filter, RANGE filterValue) {
		super(filter, filterValue);
	}

	@Override
	public final boolean checkEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return false;
		}

		return contains(block, event);
	}

	public abstract boolean contains(Block block, int event);

	@Override
	public void acceptEvent(Block block, int event) {
		// Assumption is that accept cannot be called when checkEvent returned false
		hit = true;
	}

	@Override
	public boolean isContained() {
		return hit;
	}
}
