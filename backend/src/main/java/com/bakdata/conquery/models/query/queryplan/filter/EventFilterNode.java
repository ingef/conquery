package com.bakdata.conquery.models.query.queryplan.filter;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;

public abstract class EventFilterNode<FILTER_VALUE> extends FilterNode<FILTER_VALUE> {


	private boolean hit = false;
	private boolean[] hits = null;
	private QueryExecutionContext context;

	public EventFilterNode(FILTER_VALUE filterValue) {
		super(filterValue);
	}

	@Override
	public void nextBlock(Bucket bucket) {
		// TODO this is a race-condition
		if (!context.getHitCache().contains(bucket, getFilterValue())) {
			hits = new boolean[bucket.getNumberOfEvents()];

			for (int event = 0; event < bucket.getNumberOfEvents(); event++) {
				hits[event] = checkEvent(bucket, event);
			}

			context.getHitCache().put(bucket, (FilterValue) getFilterValue(), hits);
		}
		else {
			hits = context.getHitCache().get(bucket, getFilterValue());
		}
	}

	public final boolean included(Bucket bucket, int event) {
		//TODO assert Buckets equal
		return hits[event];
	}

	public abstract boolean checkEvent(Bucket bucket, int event);

	@Override
	public final void acceptEvent(Bucket bucket, int event) {
		hit = true;
	}

	public final boolean isContained() {
		return hit;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		hit = false;
		this.context = context;
	}


}
