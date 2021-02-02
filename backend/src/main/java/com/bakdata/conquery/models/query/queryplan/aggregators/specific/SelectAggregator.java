package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.specific.string.StringType;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;


/**
 * Aggregator counting the number of occurrences of a selected value in a column.
 */
public class SelectAggregator extends SingleColumnAggregator<Long> {

	private final String selected;
	private long hits = 0;
	private int selectedId = -1;

	public SelectAggregator(Column column, String selected) {
		super(column);
		this.selected = selected;
	}

	@Override
	public void nextBlock(Bucket bucket) {
		selectedId = ((StringType) getColumn().getTypeFor(bucket)).getId(selected);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (selectedId == -1) {
			return;
		}

		if (!bucket.has(event, getColumn())) {
			return;
		}

		int value = bucket.getString(event, getColumn());

		if (value == selectedId) {
			hits++;
		}
	}

	@Override
	public Long getAggregationResult() {
		return hits > 0 ? hits : null;
	}

	@Override
	public SelectAggregator doClone(CloneContext ctx) {
		return new SelectAggregator(getColumn(), selected);
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return super.isOfInterest(bucket) &&
			   ((StringType) bucket.getStores()[getColumn().getPosition()]).getId(selected) != -1;
	}
}
