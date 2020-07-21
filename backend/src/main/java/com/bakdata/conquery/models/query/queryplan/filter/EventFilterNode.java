package com.bakdata.conquery.models.query.queryplan.filter;

import com.bakdata.conquery.models.events.Bucket;
import lombok.Getter;
import lombok.Setter;

public abstract class EventFilterNode<FILTER_VALUE> extends FilterNode<FILTER_VALUE> {

	private boolean hit = false;

	@Setter @Getter
	protected FILTER_VALUE filterValue;

	public EventFilterNode(FILTER_VALUE filter_value) {
		super(filter_value);
	}

	public abstract boolean checkEvent(Bucket bucket, int event);

	@Override
	public final void acceptEvent(Bucket bucket, int event) {
		hit = true;
	}

	public final boolean isContained(){
		return hit;
	}
	
}
