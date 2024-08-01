package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import lombok.ToString;

/**
 * Aggregator counting the occurrence of multiple values.
 */
@ToString(callSuper = true, of = "selection")
public class MultiSelectAggregator extends SingleColumnAggregator<Map<String, Integer>> {

	private final String[] selection;
	private final int[] hits;

	public MultiSelectAggregator(Column column, String[] selection) {
		super(column);
		this.selection = selection;
		this.hits = new int[selection.length];
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		Arrays.fill(hits, 0);
	}

	@Override
	public void nextBlock(Bucket bucket) {
	}

	@Override
	public void consumeEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		String stringToken = bucket.getString(event, getColumn());

		for (int index = 0; index < selection.length; index++) {
			if (Objects.equals(selection[index], stringToken)) {
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
	public boolean isOfInterest(Bucket bucket) {
//TODO
		//		for (String selected : selection) {
//			if (((StringStore) bucket.getStores()[getColumn().getPosition()]).getId(selected) == -1) {
//				return false;
//			}
//		}

		return super.isOfInterest(bucket);
	}
}
