package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.SingleColumnFilterNode;
import com.bakdata.conquery.models.types.specific.IStringType;

/**
 * Entity is included when the number of values for a specified column are within a given range.
 */
public class MultiSelectFilterNode extends SingleColumnFilterNode<String[]> {

	private int[] selectedValues;
	private boolean hit;

	public MultiSelectFilterNode(Column column, String[] filterValue) {
		super(column, filterValue);
		this.selectedValues = new int[filterValue.length];
	}

	@Override
	public void nextBlock(Block block) {
		IStringType type = (IStringType) getColumn().getTypeFor(block);

		for (int index = 0; index < filterValue.length; index++) {
			String select = filterValue[index];
			Integer parsed = type.getStringId(select);
			selectedValues[index] = parsed;
		}
	}


	@Override
	public boolean checkEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return false;
		}

		int stringToken = block.getString(event, getColumn());

		for (int selectedValue : selectedValues) {
			if (selectedValue == stringToken) {
				return true;
			}
		}

		return false;
	}

	@Override
	public MultiSelectFilterNode doClone(CloneContext ctx) {
		return new MultiSelectFilterNode(getColumn(), filterValue);
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
