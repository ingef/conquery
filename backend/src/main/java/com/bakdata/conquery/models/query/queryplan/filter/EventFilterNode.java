package com.bakdata.conquery.models.query.queryplan.filter;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;

public abstract class EventFilterNode<FILTER_VALUE> extends FilterNode<FILTER_VALUE> {

	private boolean hit = false;

	public EventFilterNode(FILTER_VALUE filterValue) {
		super(filterValue);
	}

	public abstract boolean checkEvent(Bucket bucket, int event);

	@Override
	public final boolean acceptEvent(Bucket bucket, int event) {
		hit = true;
		return true;
	}

	public final boolean isContained() {
		return hit;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		hit = false;
	}


}
