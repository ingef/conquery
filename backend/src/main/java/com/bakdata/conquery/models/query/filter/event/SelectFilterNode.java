package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.types.specific.IStringType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SelectFilterNode extends FilterNode<String, SelectFilter> {

	private int selectedId = -1;
	private boolean hit = false;

	public SelectFilterNode(SelectFilter filter, String filterValue) {
		super(filter, filterValue);
	}

	@Override
	public void nextBlock(Block block) {
		//you can then also skip the block if the id is -1
		selectedId = ((IStringType) filter.getColumn().getTypeFor(block)).getStringId(filterValue);
	}

	@Override
	public SelectFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new SelectFilterNode(filter, filterValue);
	}

	@Override
	public boolean checkEvent(Block block, int event) {
		if (selectedId == -1 || !block.has(event, filter.getColumn())) {
			return false;
		}

		int value = block.getString(event, filter.getColumn());

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
