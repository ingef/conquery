package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.types.specific.IStringType;

/**
 * Entity is included when the number of values for a specified column are within a given range.
 */
public class MultiSelectFilterNode<FILTER extends SingleColumnFilter<String[]>> extends FilterNode<String[], FILTER> {

	private int[] selectedValues;
	private boolean hit;

	public MultiSelectFilterNode(FILTER filter, String[] filterValue) {
		super(filter, filterValue);
		this.selectedValues = new int[filterValue.length];
	}


	@Override
	public void nextBlock(Block block) {
		IStringType type = (IStringType) filter.getColumn().getTypeFor(block);

		for (int index = 0; index < filterValue.length; index++) {
			String select = filterValue[index];
			Integer parsed = type.getStringId(select);
			selectedValues[index] = parsed;
		}
	}


	@Override
	public boolean checkEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return false;
		}

		int stringToken = block.getString(event, filter.getColumn());

		for (int selectedValue : selectedValues) {
			if (selectedValue == stringToken) {
				return true;
			}
		}

		return false;
	}

	@Override
	public FilterNode<?, ?> clone(QueryPlan plan, QueryPlan clone) {
		return new MultiSelectFilterNode<>(filter, filterValue);
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
