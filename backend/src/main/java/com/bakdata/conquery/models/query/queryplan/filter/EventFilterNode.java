package com.bakdata.conquery.models.query.queryplan.filter;

import com.bakdata.conquery.models.events.Bucket;

public abstract class EventFilterNode<FILTER_VALUE> extends FilterNode<FILTER_VALUE> {

	private boolean hit = false;

	public EventFilterNode(FILTER_VALUE filterValue) {
		super(filterValue);
	}

	@Override
	public final void acceptEvent(Bucket bucket, int event) {
		hit = true;
	}

	public final boolean isContained(){
		return hit;
	}
	
}
