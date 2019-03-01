package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.multiset.HashMultiSet;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.types.specific.IStringType;


public class MultiSelectAggregator extends SingleColumnAggregator<MultiSet<String>> {

	private final String[] selection;
	private final int[] hits;
	private int[] selectedValues;

	public MultiSelectAggregator(Column column, String[] selection) {
		super(column);
		this.selection = selection;
		this.selectedValues = new int[selection.length];
		this.hits = new int[selection.length];
	}

	@Override
	public void nextBlock(Block block) {
		IStringType type = (IStringType) getColumn().getTypeFor(block);

		for (int index = 0; index < selection.length; index++) {
			selectedValues[index] = type.getStringId(selection[index]);
		}
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		int stringToken = block.getString(event, getColumn());

		for (int index = 0; index < selectedValues.length; index++) {
			if (selectedValues[index] == stringToken) {
				hits[index]++;
				return;
			}
		}
	}

	@Override
	public MultiSet<String> getAggregationResult() {
		MultiSet<String> out = new HashMultiSet<>();

		for (int i = 0; i < hits.length; i++) {
			int hit = hits[i];
			if (hit > 0) {
				out.add(selection[i], hit);
			}
		}

		return out;
	}

	@Override
	public MultiSelectAggregator clone() {
		return new MultiSelectAggregator(getColumn(), selection);
	}
}
