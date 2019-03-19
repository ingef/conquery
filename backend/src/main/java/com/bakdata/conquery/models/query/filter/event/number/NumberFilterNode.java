package com.bakdata.conquery.models.query.filter.event.number;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.filter.SingleColumnFilterNode;

public abstract class NumberFilterNode<RANGE extends IRange<?, ?>> extends SingleColumnFilterNode<RANGE> {

	private boolean hit;

	public NumberFilterNode(Column column, RANGE filterValue) {
		super(column, filterValue);
	}

	@Override
	public final boolean checkEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
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
