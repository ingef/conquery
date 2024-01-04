package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.ToString;

/**
 * Aggregator counting the occurrence of multiple values.
 */
@ToString(callSuper = true, of = "selection")
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
	public void init(Entity entity, QueryExecutionContext context) {
		Arrays.fill(hits, 0);
	}

	@Override
	public void nextBlock(Bucket bucket) {
		StringStore type = (StringStore) bucket.getStore(getColumn());

		for (int index = 0; index < selection.length; index++) {
			selectedValues[index] = type.getId(selection[index]);
		}
	}

	@Override
	public void consumeEvent(Bucket bucket, int event) {
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
	public Map<String, Integer> createAggregationResult() {
		Map<String, Integer> out = new HashMap<>();

		for (int i = 0; i < hits.length; i++) {
			int hit = hits[i];
			if (hit > 0) {
				out.merge(selection[i], hit, Integer::sum);
			}
		}

		return out.isEmpty() ? null : out;
	}

	@Override
	public ResultType getResultType() {
		return ResultType.StringT.INSTANCE;
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		for (String selected : selection) {
			if (((StringStore) bucket.getStores()[getColumn().getPosition()]).getId(selected) == -1) {
				return false;
			}
		}

		return super.isOfInterest(bucket);
	}
}
