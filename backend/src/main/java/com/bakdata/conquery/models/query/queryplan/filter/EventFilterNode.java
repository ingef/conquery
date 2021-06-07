package com.bakdata.conquery.models.query.queryplan.filter;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.EventIterating;
import com.bakdata.conquery.models.query.queryplan.clone.CtxCloneable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

@Data
@AllArgsConstructor
@Setter
public abstract class EventFilterNode<FILTER_VALUE> implements EventIterating, CtxCloneable<EventFilterNode<FILTER_VALUE>> {

	private FILTER_VALUE filterValue;


	public abstract boolean checkEvent(Bucket bucket, int event);

	@Override
	public final void acceptEvent(Bucket bucket, int event) {

	}
}
