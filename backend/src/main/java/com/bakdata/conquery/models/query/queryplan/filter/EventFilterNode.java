package com.bakdata.conquery.models.query.queryplan.filter;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;

public abstract non-sealed class EventFilterNode<FILTER_VALUE> extends FilterNode<FILTER_VALUE> {

	public EventFilterNode(FILTER_VALUE filterValue) {
		super(filterValue);
	}

	//TODO rename to acceptEvent?
	public abstract boolean checkEvent(Bucket bucket, int event);

	@Override
	public final boolean acceptEvent(Bucket bucket, int event) {
		throw new IllegalStateException("May not be called.");
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
	}


}
