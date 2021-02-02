package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator counting the occurrence of multiple values.
 */
public class MultiSelectAggregator extends SingleColumnAggregator<Map<String, Integer>> {

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
	public void nextBlock(Bucket bucket) {
		StringStore type = (StringStore) getColumn().getTypeFor(bucket);

		for (int index = 0; index < selection.length; index++) {
			selectedValues[index] = type.getId(selection[index]);
		}
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		int stringToken = bucket.getString(event, getColumn());

		for (int index = 0; index < selectedValues.length; index++) {
			if (selectedValues[index] == stringToken) {
				hits[index]++;
				return;
			}
		}
	}

	@Override
	public Map<String, Integer> getAggregationResult() {
		Map<String, Integer> out = new HashMap<>();

		for (int i = 0; i < hits.length; i++) {
			int hit = hits[i];
			if (hit > 0) {
				out.merge(selection[i], hit, (a,b)->a+b);
			}
		}

		return out.isEmpty() ? null : out;
	}

	@Override
	public MultiSelectAggregator doClone(CloneContext ctx) {
		return new MultiSelectAggregator(getColumn(), selection);
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.STRING;
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		for (String selected : selection) {
			if(((StringStore) bucket.getStores()[getColumn().getPosition()]).getId(selected) == -1) {
				return false;
			}
		}

		return super.isOfInterest(bucket);
	}
}
