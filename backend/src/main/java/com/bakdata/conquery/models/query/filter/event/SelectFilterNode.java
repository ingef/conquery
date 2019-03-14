package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.SingleColumnFilterNode;
import com.bakdata.conquery.models.types.specific.IStringType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SelectFilterNode extends SingleColumnFilterNode<String> {

	private int selectedId = -1;
	private boolean hit = false;

	public SelectFilterNode(Column column, String filterValue) {
		super(column, filterValue);
	}

	@Override
	public void nextBlock(Block block) {
		//you can then also skip the block if the id is -1
		selectedId = ((IStringType) getColumn().getTypeFor(block)).getStringId(filterValue);
	}

	@Override
	public SelectFilterNode doClone(CloneContext ctx) {
		return new SelectFilterNode(getColumn(), filterValue);
	}

	@Override
	public boolean checkEvent(Block block, int event) {
		if (selectedId == -1 || !block.has(event, getColumn())) {
			return false;
		}

		int value = block.getString(event, getColumn());

		return value == selectedId;
	}

	@Override
	public void acceptEvent(Block block, int event) {
		this.hit = true;
	}

	@Override
	public boolean isContained() {
		return hit;
	}
}
