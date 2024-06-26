package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Objects;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import lombok.ToString;


/**
 * Aggregator counting the number of occurrences of a selected value in a column.
 */
@ToString(callSuper = true, of = {"selected"})
public class SelectAggregator extends SingleColumnAggregator<Long> {

	private final String selected;
	private long hits = 0;

	public SelectAggregator(Column column, String selected) {
		super(column);
		this.selected = selected;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		hits = 0;
	}

	@Override
	public void nextBlock(Bucket bucket) {
	}

	@Override
	public void consumeEvent(Bucket bucket, int event) {

		if (!bucket.has(event, getColumn())) {
			return;
		}

		final String value = bucket.getString(event, getColumn());

		if (Objects.equals(value, selected)) {
			hits++;
		}
	}

	@Override
	public Long createAggregationResult() {
		return hits > 0 ? hits : null;
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return super.isOfInterest(bucket);
			   //TODO  && ((StringStore) bucket.getStores()[getColumn().getPosition()]).getId(selected) != -1;
	}
}
