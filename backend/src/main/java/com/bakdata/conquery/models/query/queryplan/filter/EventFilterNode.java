package com.bakdata.conquery.models.query.queryplan.filter;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;

public abstract class EventFilterNode<FILTER_VALUE> extends FilterNode<FILTER_VALUE> {

	private boolean hit = false;
	private boolean[] hits = null;
	private int size = -1;

	public EventFilterNode(FILTER_VALUE filterValue) {
		super(filterValue);
	}

	@Override
	public void nextBlock(Bucket bucket) {
		if (size < bucket.getNumberOfEvents()) {
			size = bucket.getNumberOfEvents();
			hits = new boolean[size];
		}

		for (int event = 0; event < bucket.getNumberOfEvents(); event++) {
			hits[event] = checkEvent(bucket, event);
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
	}


}
